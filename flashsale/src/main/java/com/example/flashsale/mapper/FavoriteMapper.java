package com.example.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.flashsale.entity.Favorite;
import com.example.flashsale.entity.GoodsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 查询用户收藏的所有秒杀商品列表
     */
    @Select("SELECT g.id, g.goods_name AS goodsName, g.goods_img AS goodsImg, " +
            "g.goods_detail AS goodsDetail, g.goods_price AS goodsPrice, " +
            "sg.seckill_price AS seckillPrice, sg.stock_count AS stockCount, " +
            "sg.start_time AS startDate, sg.end_time AS endDate " +
            "FROM t_favorite f " +
            "JOIN t_goods g ON f.goods_id = g.id " +
            "LEFT JOIN t_seckill_goods sg ON g.id = sg.goods_id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.create_time DESC")
    List<GoodsVo> findFavoriteGoodsByUserId(@Param("userId") Long userId);
}