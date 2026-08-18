package com.example.flashsale.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.flashsale.entity.Favorite;
import com.example.flashsale.entity.GoodsVo;
import com.example.flashsale.mapper.FavoriteMapper;
import com.example.flashsale.utils.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Resource
    private FavoriteMapper favoriteMapper;

    /**
     * 1. 切换收藏状态
     */
    @PostMapping("/toggle")
    public R<Boolean> toggleFavorite(@RequestParam("userId") Long userId, @RequestParam("goodsId") Long goodsId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getGoodsId, goodsId);

        Favorite exist = favoriteMapper.selectOne(wrapper);
        if (exist != null) {
            // 已存在，取消收藏
            favoriteMapper.deleteById(exist.getId());
            return R.success("已取消收藏", false);
        } else {
            // 不存在，新增收藏
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setGoodsId(goodsId);
            fav.setCreateTime(new Date());
            favoriteMapper.insert(fav);
            return R.success("收藏成功！", true);
        }
    }

    /**
     * 2. 查询商品是否已被当前用户收藏
     */
    @GetMapping("/status")
    public R<Boolean> checkFavoriteStatus(@RequestParam("userId") Long userId, @RequestParam("goodsId") Long goodsId) {
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getGoodsId, goodsId)
        );
        return R.success("查询成功", count > 0);
    }

    /**
     * 3. 获取我的收藏商品列表
     */
    @GetMapping("/list")
    public R<List<GoodsVo>> listMyFavorites(@RequestParam("userId") Long userId) {
        List<GoodsVo> list = favoriteMapper.findFavoriteGoodsByUserId(userId);
        return R.success("获取成功", list);
    }
}