package com.example.flashsale.service;

import com.example.flashsale.config.RabbitMQConfig;
import com.example.flashsale.entity.Order;
import com.example.flashsale.entity.SeckillGoods;
import com.example.flashsale.entity.SeckillOrder;
import com.example.flashsale.mapper.OrderMapper;
import com.example.flashsale.mapper.SeckillGoodsMapper;
import com.example.flashsale.mapper.SeckillOrderMapper;
import com.example.flashsale.utils.SeckillMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
public class SeckillQueueListener {

    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private SeckillOrderMapper seckillOrderMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 1. 监听秒杀排队队列，处理异步下单
     */
    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleSeckillMessage(SeckillMessage message) {
        Long userId = message.getUserId();
        Long goodsId = message.getGoodsId();

        try {
            // 1. 校验 DB 扣减库存 (WHERE stock_count > 0 保证底线)
            SeckillGoods goods = seckillGoodsMapper.selectById(goodsId);
            if (goods == null || goods.getStockCount() <= 0) {
                stringRedisTemplate.opsForValue().set("seckill:result:" + userId + ":" + goodsId, "FAIL");
                return;
            }

            // 2. 扣减 MySQL 实际库存
            goods.setStockCount(goods.getStockCount() - 1);
            seckillGoodsMapper.updateById(goods);

            // 3. 生成主订单
            Order order = new Order();
            order.setUserId(userId);
            order.setGoodsId(goodsId);
            order.setGoodsName("秒杀商品_" + goodsId);
            order.setGoodsCount(1);
            order.setOrderPrice(goods.getSeckillPrice());
            order.setStatus(0); // 0: 未支付
            order.setCreateTime(LocalDateTime.now());
            orderMapper.insert(order);

            // 4. 生成秒杀关联订单 (依赖 uk_user_goods 唯一索引防重)
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setUserId(userId);
            seckillOrder.setGoodsId(goodsId);
            seckillOrder.setOrderId(order.getId());
            seckillOrderMapper.insert(seckillOrder);

            // 5. 写入抢购成功状态与订单号到 Redis
            stringRedisTemplate.opsForValue().set("seckill:result:" + userId + ":" + goodsId, order.getId().toString());

            // 6. 发送延迟取消订单消息
            log.info("【下单成功】订单ID: {} 已创建，投递 15 分钟延迟检查消息到 RabbitMQ", order.getId());
            rabbitTemplate.convertAndSend(RabbitMQConfig.DELAY_EXCHANGE, RabbitMQConfig.DELAY_ROUTING_KEY, order.getId(), msg -> {
                msg.getMessageProperties().setDelay(15 * 60 * 1000);
                return msg;
            });

        } catch (DuplicateKeyException e) {
            // 捕获唯一索引重复冲突（幂等兜底）
            log.warn("【重复下单】用户: {} 商品: {} 触发数据库唯一索引拦截", userId, goodsId);
            stringRedisTemplate.opsForValue().set("seckill:result:" + userId + ":" + goodsId, "FAIL");
        } catch (Exception e) {
            log.error("【下单异常】处理秒杀消息失败", e);
            stringRedisTemplate.opsForValue().set("seckill:result:" + userId + ":" + goodsId, "FAIL");
            throw e; // 触发事务回滚
        }
    }

    /**
     * 2. 监听延迟队列，处理超时未支付自动取消订单
     */
    @RabbitListener(queues = RabbitMQConfig.DELAY_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderTimeout(Long orderId) {
        log.info("【超时检查】收到订单ID: {} 的延迟检查任务...", orderId);

        // 1. 查询订单信息
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("【超时检查】未找到订单ID: {}", orderId);
            return;
        }

        // 2. 判断订单状态：仅对 0 (未支付) 状态的订单进行超时取消
        if (order.getStatus() == 0) {
            log.info("【超时取消】订单ID: {} 超时未支付，开始自动取消并恢复库存...", orderId);

            // A. 更新数据库主订单状态为已取消 (假设 2 表示已取消/超时关闭)
            order.setStatus(2);
            orderMapper.updateById(order);

            // B. 恢复 MySQL 数据库商品库存
            SeckillGoods goods = seckillGoodsMapper.selectById(order.getGoodsId());
            if (goods != null) {
                goods.setStockCount(goods.getStockCount() + 1);
                seckillGoodsMapper.updateById(goods);
            }

            // C. 恢复 Redis 预扣库存
            String stockKey = "seckill:stock:" + order.getGoodsId();
            stringRedisTemplate.opsForValue().increment(stockKey);

            // D. 扣减 Redis 中用户的已购计数（允许用户重新抢购）
            String userKey = "seckill:users:" + order.getGoodsId();
            Long currentCount = stringRedisTemplate.opsForHash().increment(userKey, order.getUserId().toString(), -1);
            if (currentCount <= 0) {
                stringRedisTemplate.opsForHash().delete(userKey, order.getUserId().toString());
            }

            // E. 删除秒杀关联订单记录（释放数据库唯一索引 constraint）
            seckillOrderMapper.deleteById(orderId);

            log.info("【超时取消】订单ID: {} 取消成功！Redis/MySQL 库存与购买资格已全量恢复！", orderId);
        } else {
            log.info("【超时检查】订单ID: {} 状态为 {}，非未支付状态，无需处理。", orderId, order.getStatus());
        }
    }
}