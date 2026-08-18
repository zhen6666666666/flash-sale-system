package com.example.flashsale.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.flashsale.entity.Order;
import com.example.flashsale.entity.SeckillGoods;
import com.example.flashsale.mapper.OrderMapper;
import com.example.flashsale.mapper.SeckillGoodsMapper;
import com.example.flashsale.utils.R;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @GetMapping("/userList")
    public R<List<Order>> getUserOrders(@RequestParam("userId") Long userId) {
        List<Order> list = orderMapper.selectUserOrdersWithGoodsName(userId);
        return R.success("获取成功", list);
    }

    /**
     * 模拟支付订单接口
     */
    @PostMapping("/pay")
    public R<String> payOrder(@RequestParam("orderId") Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return R.error("订单不存在");
        }
        if (order.getStatus() != 0) {
            return R.error("订单已支付或已失效，请勿重复支付");
        }

        // 修改状态为 1 并写入当前支付时间
        order.setStatus(1);
        order.setPayTime(new Date());
        orderMapper.updateById(order);

        return R.success("支付成功");
    }

    /**
     * 取消订单接口
     */
    @PostMapping("/cancel")
    @Transactional(rollbackFor = Exception.class)
    public R<String> cancelOrder(@RequestParam("orderId") Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return R.error("订单不存在");
        }
        if (order.getStatus() != 0) {
            return R.error("当前订单状态无法取消");
        }

        // 更新订单状态为已取消/失效（设为 2）
        order.setStatus(2);
        orderMapper.updateById(order);

        // 回滚 MySQL 及 Redis 中的商品库存
        if (order.getGoodsId() != null) {
            SeckillGoods sg = seckillGoodsMapper.selectOne(
                    new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getGoodsId, order.getGoodsId())
            );
            if (sg != null) {
                sg.setStockCount(sg.getStockCount() + 1);
                seckillGoodsMapper.updateById(sg);
            }
            // Redis 库存 + 1
            stringRedisTemplate.opsForValue().increment("seckill:stock:" + order.getGoodsId());
        }

        return R.success("订单已成功取消");
    }
}