package com.example.flashsale.service.impl;

import com.example.flashsale.config.RabbitMQConfig;
import com.example.flashsale.entity.GoodsVo;
import com.example.flashsale.entity.SeckillGoods;

import com.example.flashsale.mapper.SeckillGoodsMapper;
import com.example.flashsale.service.SeckillService;
import com.example.flashsale.utils.R;
import com.example.flashsale.utils.SeckillMessage;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void warmupGoods(Long goodsId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(goodsId);
        if (goods != null) {
            // 预热库存到 Redis: seckill:stock:{goodsId}
            stringRedisTemplate.opsForValue().set("seckill:stock:" + goodsId, goods.getStockCount().toString());
            // 预热限购信息: seckill:limit:{goodsId}
            stringRedisTemplate.opsForValue().set("seckill:limit:" + goodsId, goods.getLimitCount().toString());
        }
    }

    @Override
    public String getPathToken(Long userId, Long goodsId) {
        String str = DigestUtils.md5DigestAsHex((userId + "_" + goodsId + "_" + UUID.randomUUID()).getBytes());
        // 隐藏地址 Token 缓存 60 秒
        stringRedisTemplate.opsForValue().set("seckill:path:" + userId + ":" + goodsId, str, 60, TimeUnit.SECONDS);
        return str;
    }

    @Override
    public R<String> doSeckill(Long userId, Long goodsId, String pathToken, String requestId) {
        // 1. 校验动态路径 Token
        String realToken = stringRedisTemplate.opsForValue().get("seckill:path:" + userId + ":" + goodsId);
        if (realToken == null || !realToken.equals(pathToken)) {
            return R.error("非法请求，接口校验失败");
        }

        // 2. 检查防重 Key (requestId)
        Boolean setIfAbsent = stringRedisTemplate.opsForValue().setIfAbsent("seckill:req:" + requestId, "1", 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(setIfAbsent)) {
            return R.error("请勿重复提交");
        }

        // 3. 读取限购数量
        String limitStr = stringRedisTemplate.opsForValue().get("seckill:limit:" + goodsId);
        int limitCount = limitStr == null ? 1 : Integer.parseInt(limitStr);

        // 4. 执行 Lua 脚本预扣库存
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill.lua")));
        script.setResultType(Long.class);

        List<String> keys = List.of("seckill:stock:" + goodsId, "seckill:users:" + goodsId);
        Long result = stringRedisTemplate.execute(script, keys, userId.toString(), String.valueOf(limitCount));

        if (result == null || result == -2) {
            return R.error("手慢无，商品已被抢光！");
        }
        if (result == -1) {
            return R.error("您已达到该商品的限购上限！");
        }

        // 5. Lua 预扣成功，发送 MQ 异步下单
        SeckillMessage message = new SeckillMessage(userId, goodsId, requestId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.SECKILL_EXCHANGE, RabbitMQConfig.SECKILL_ROUTING_KEY, message);

        return R.success("排队抢购中，请稍候...", null);
    }

    @Override
    public R<Long> getSeckillResult(Long userId, Long goodsId) {
        String result = stringRedisTemplate.opsForValue().get("seckill:result:" + userId + ":" + goodsId);
        if (result == null) {
            return R.success("排队中", 0L); // 0 代表还在排队中
        } else if ("FAIL".equals(result)) {
            return R.error("抢购失败，商品已售罄");
        } else {
            return R.success("抢购成功", Long.parseLong(result)); // 返回真实的 orderId
        }
    }

    @Override
    public List<GoodsVo> listGoodsVo() {
        return seckillGoodsMapper.findGoodsVo(null);
    }

    @Override
    public List<GoodsVo> listGoodsVo(String keyword) {
        return seckillGoodsMapper.findGoodsVo(keyword);
    }
}