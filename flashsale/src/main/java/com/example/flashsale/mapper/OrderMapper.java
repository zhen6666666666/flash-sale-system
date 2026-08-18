package com.example.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.flashsale.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 查询用户订单列表
     */
    @Select("SELECT o.id, " +
            "COALESCE(g.goods_name, o.goods_name) AS goodsName, " +
            "o.order_price AS orderPrice, " +
            "o.create_time AS createTime, " +
            "o.status " +
            "FROM t_order o " +
            "LEFT JOIN t_goods g ON o.goods_id = g.id " +
            "WHERE o.user_id = #{userId} " +
            "ORDER BY o.create_time DESC")
    List<Order> selectUserOrdersWithGoodsName(@Param("userId") Long userId);
}