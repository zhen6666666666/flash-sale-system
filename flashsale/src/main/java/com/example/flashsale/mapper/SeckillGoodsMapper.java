package com.example.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.flashsale.entity.GoodsVo;
import com.example.flashsale.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /**
     * 秒杀商品联表查询
     */
    @Select("<script>" +
            "SELECT g.id, g.goods_name AS goodsName, g.goods_img AS goodsImg, " +
            "g.goods_detail AS goodsDetail, g.goods_price AS goodsPrice, " +
            "sg.seckill_price AS seckillPrice, sg.stock_count AS stockCount, " +
            "sg.start_time AS startDate, sg.end_time AS endDate " +
            "FROM t_seckill_goods sg " +
            "LEFT JOIN t_goods g ON sg.goods_id = g.id " +
            "<where>" +
            "  <if test=\"keyword != null and keyword != ''\">" +
            "    (g.goods_name LIKE CONCAT('%', #{keyword}, '%') OR g.goods_detail LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "</script>")
    List<GoodsVo> findGoodsVo(@Param("keyword") String keyword);
}