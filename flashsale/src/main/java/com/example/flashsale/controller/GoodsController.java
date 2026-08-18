package com.example.flashsale.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.flashsale.dto.SeckillGoodsDTO;
import com.example.flashsale.entity.Goods;
import com.example.flashsale.entity.GoodsVo;
import com.example.flashsale.entity.SeckillGoods;
import com.example.flashsale.mapper.GoodsMapper;
import com.example.flashsale.mapper.SeckillGoodsMapper;
import com.example.flashsale.service.SeckillService;
import com.example.flashsale.utils.R;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private SeckillService seckillService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 1. 发布秒杀商品
     */
    @PostMapping("/publish")
    @Transactional(rollbackFor = Exception.class)
    public R<String> publishSeckillGoods(@RequestBody SeckillGoodsDTO dto) {
        Goods goods = new Goods();
        goods.setMerchantId(dto.getMerchantId());
        goods.setGoodsName(dto.getGoodsName());
        goods.setGoodsImg(dto.getGoodsImg());
        goods.setGoodsPrice(dto.getGoodsPrice());
        goods.setGoodsStock(dto.getStockCount());

        if (dto.getGoodsDetail() != null && !dto.getGoodsDetail().trim().isEmpty()) {
            goods.setGoodsDetail(dto.getGoodsDetail());
        } else {
            goods.setGoodsDetail(dto.getGoodsName() + " 热门秒杀商品");
        }

        goods.setCreateTime(LocalDateTime.now());
        goodsMapper.insert(goods);

        SeckillGoods sg = new SeckillGoods();
        sg.setGoodsId(goods.getId());
        sg.setSeckillPrice(dto.getSeckillPrice());
        sg.setStockCount(dto.getStockCount());
        sg.setLimitCount(dto.getLimitCount() != null ? dto.getLimitCount() : 1);
        sg.setStartTime(dto.getStartTime());
        sg.setEndTime(dto.getEndTime());
        sg.setStatus(1);
        seckillGoodsMapper.insert(sg);

        Long goodsId = goods.getId();
        stringRedisTemplate.opsForValue().set("seckill:stock:" + goodsId, String.valueOf(dto.getStockCount()));
        stringRedisTemplate.opsForValue().set("seckill:limit:" + goodsId, String.valueOf(sg.getLimitCount()));

        return R.success("秒杀商品发布成功！");
    }

    /**
     * 2. 获取当前商家的售卖列表
     */
    @GetMapping("/merchantList")
    public R<List<GoodsVo>> getMerchantGoodsList(@RequestParam("merchantId") Long merchantId) {
        // 调用 SeckillService 获取所有，然后通过 stream 过滤出当前商家的商品
        List<GoodsVo> allGoods = seckillService.listGoodsVo();

        List<GoodsVo> merchantGoods = allGoods.stream().filter(g -> {
            Goods goods = goodsMapper.selectById(g.getId());
            return goods != null && merchantId.equals(goods.getMerchantId());
        }).toList();

        return R.success("获取成功", merchantGoods);
    }

    /**
     * 3. 编辑/修改秒杀商品
     */
    @PostMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateSeckillGoods(@RequestBody SeckillGoodsDTO dto) {
        if (dto.getGoodsId() == null) {
            return R.error("商品 ID 不能为空");
        }

        // 校验秒杀状态
        SeckillGoods sg = seckillGoodsMapper.selectOne(
                new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getGoodsId, dto.getGoodsId())
        );

        if (sg != null) {
            long now = System.currentTimeMillis();

            // 🌟 将 LocalDateTime 转为毫秒时间戳进行大小比较
            long start = sg.getStartTime() != null ?
                    sg.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0;

            if (now >= start) {
                return R.error("正在秒杀或秒杀已开始/结束的商品不可编辑！");
            }

            // 更新秒杀主表信息
            sg.setSeckillPrice(dto.getSeckillPrice());
            sg.setStockCount(dto.getStockCount());
            sg.setLimitCount(dto.getLimitCount() != null ? dto.getLimitCount() : 1);

            // 🌟 直接赋值 LocalDateTime
            sg.setStartTime(dto.getStartTime());
            sg.setEndTime(dto.getEndTime());

            seckillGoodsMapper.updateById(sg);
        }

        // 更新商品基础信息表
        Goods goods = new Goods();
        goods.setId(dto.getGoodsId());
        goods.setGoodsName(dto.getGoodsName());
        goods.setGoodsImg(dto.getGoodsImg());
        goods.setGoodsDetail(dto.getGoodsDetail());
        goods.setGoodsPrice(dto.getGoodsPrice());
        goodsMapper.updateById(goods);

        // 更新 Redis 缓存数据
        Long goodsId = dto.getGoodsId();
        stringRedisTemplate.opsForValue().set("seckill:stock:" + goodsId, String.valueOf(dto.getStockCount()));
        stringRedisTemplate.opsForValue().set("seckill:limit:" + goodsId, String.valueOf(dto.getLimitCount()));

        return R.success("商品信息更新成功！");
    }

    /**
     * 4. 下架/删除秒杀商品
     */
    @PostMapping("/delete")
    @Transactional(rollbackFor = Exception.class)
    public R<String> deleteSeckillGoods(@RequestParam("id") Long goodsId) {
        SeckillGoods sg = seckillGoodsMapper.selectOne(
                new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getGoodsId, goodsId)
        );

        if (sg != null) {
            long now = System.currentTimeMillis();

            // 🌟 将 LocalDateTime 转为毫秒时间戳进行大小比较
            long start = sg.getStartTime() != null ?
                    sg.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0;
            long end = sg.getEndTime() != null ?
                    sg.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0;

            if (now >= start && now <= end) {
                return R.error("正在秒杀中的商品无法下架！");
            }

            // 删除秒杀活动表记录
            seckillGoodsMapper.deleteById(sg.getId());
        }

        // 删除商品表记录
        goodsMapper.deleteById(goodsId);

        // 删除对应的 Redis 缓存
        stringRedisTemplate.delete("seckill:stock:" + goodsId);
        stringRedisTemplate.delete("seckill:limit:" + goodsId);

        return R.success("商品已成功下架！");
    }

    /**
     * 5. 上传商品图片接口
     */
    @PostMapping("/uploadImg")
    public R<String> uploadGoodsImg(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.error("上传文件不能为空");
        }

        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = (originalFilename != null && originalFilename.contains(".")) ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".png";
        String newFilename = UUID.randomUUID().toString() + suffix;

        try {
            file.transferTo(new File(uploadDir + newFilename));
            String imgUrl = "/api/uploads/" + newFilename;
            return R.success("图片上传成功", imgUrl);
        } catch (IOException e) {
            return R.error("图片文件保存失败: " + e.getMessage());
        }
    }
}