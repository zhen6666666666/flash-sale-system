package com.example.flashsale.service;

import com.example.flashsale.entity.GoodsVo;
import com.example.flashsale.utils.R;

import java.util.List;

public interface SeckillService {
    // 预热商品库存到 Redis
    void warmupGoods(Long goodsId);

    // 获取动态秒杀隐藏路径 Token
    String getPathToken(Long userId, Long goodsId);

    // 执行秒杀预扣减（Lua + MQ）
    R<String> doSeckill(Long userId, Long goodsId, String pathToken, String requestId);

    // 轮询查询抢购结果
    R<Long> getSeckillResult(Long userId, Long goodsId);

    List<GoodsVo> listGoodsVo(String keyword);
    List<GoodsVo> listGoodsVo();
}