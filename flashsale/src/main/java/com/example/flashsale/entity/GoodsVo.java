package com.example.flashsale.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class GoodsVo {
    private Long id;
    private String goodsName;
    private String goodsImg;
    private String goodsDetail;
    private BigDecimal goodsPrice;
    private BigDecimal seckillPrice;
    private Integer stockCount;
    private Date startDate;
    private Date endDate;
}