package com.example.flashsale.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.flashsale.entity.GoodsVo;
import com.example.flashsale.service.SeckillService;
import com.example.flashsale.utils.R;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Resource
    private SeckillService seckillService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成秒杀图形验证码
     */
    @GetMapping("/captcha")
    public void getCaptcha(@RequestParam Long userId,
                           @RequestParam Long goodsId,
                           HttpServletResponse response) throws IOException {
        if (userId == null || goodsId == null) {
            return;
        }

        // 设置响应头
        response.setContentType("image/png");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        // 生成 4 位验证码，宽 130，高 48
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        captcha.setCharType(Captcha.TYPE_DEFAULT);

        // 存入 Redis，有效期 60 秒（Key 结合 userId 和 goodsId）
        String redisKey = "seckill:captcha:" + userId + ":" + goodsId;
        stringRedisTemplate.opsForValue().set(redisKey, captcha.text().toLowerCase(), 60, TimeUnit.SECONDS);

        // 输出图片流
        captcha.out(response.getOutputStream());
    }

    // ==================== 1. 商品预热 ====================

    @PostMapping("/warmup/{goodsId}")
    public R<String> warmup(@PathVariable Long goodsId) {
        seckillService.warmupGoods(goodsId);
        return R.success("商品预热成功");
    }


    // ==================== 2. 获取隐藏秒杀路径 Token（支持验证码 + Sentinel 限流） ====================

    @GetMapping("/getPath")
    @SentinelResource(value = "getPathRule", blockHandler = "getPathBlockHandler")
    public R<String> getPath(@RequestParam Long userId,
                             @RequestParam Long goodsId,
                             @RequestParam(required = false) String captchaCode) {

        // 1. 校验图形验证码
        if (captchaCode == null || captchaCode.trim().isEmpty()) {
            return R.error("请输入验证码");
        }
        String redisKey = "seckill:captcha:" + userId + ":" + goodsId;
        String realCaptcha = stringRedisTemplate.opsForValue().get(redisKey);

        if (realCaptcha == null || !realCaptcha.equalsIgnoreCase(captchaCode.trim())) {
            return R.error("验证码不正确或已过期");
        }

        // 验证码验证成功后立即删除，防止重复利用
        stringRedisTemplate.delete(redisKey);

        // 2. 生成秒杀 Token
        String token = seckillService.getPathToken(userId, goodsId);
        return R.success(token);
    }

    /**
     * getPath 的 Sentinel 限流降级方法
     */
    public R<String> getPathBlockHandler(Long userId, Long goodsId, String captchaCode, BlockException ex) {
        return R.error("网络开小差了，获取抢购资格人数过多，请稍后再试！");
    }


    // ==================== 3. 执行秒杀  ====================

    @PostMapping("/{pathToken}/doSeckill")
    @SentinelResource(value = "doSeckillRule", blockHandler = "doSeckillBlockHandler")
    public R<String> doSeckill(@PathVariable String pathToken,
                               @RequestParam Long userId,
                               @RequestParam Long goodsId,
                               @RequestParam String requestId) {
        return seckillService.doSeckill(userId, goodsId, pathToken, requestId);
    }

    /**
     * doSeckill 的 Sentinel 限流降级方法
     */
    public R<String> doSeckillBlockHandler(String pathToken, Long userId, Long goodsId, String requestId, BlockException ex) {
        return R.error("秒杀通道拥堵，系统已自动拦截，请重试！");
    }


    // ==================== 4. 轮询秒杀结果 ====================

    @GetMapping("/result")
    public R<Long> getResult(@RequestParam Long userId, @RequestParam Long goodsId) {
        return seckillService.getSeckillResult(userId, goodsId);
    }

    /**
     * 获取秒杀商品列表（支持搜索）
     */
    @GetMapping("/list")
    public R<List<GoodsVo>> getGoodsList(@RequestParam(value = "keyword", required = false) String keyword) {
        List<GoodsVo> list = seckillService.listGoodsVo(keyword);
        return R.success("获取商品列表成功", list);
    }
}