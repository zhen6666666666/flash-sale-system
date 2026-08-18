package com.example.flashsale.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillGoodsDTO {
    private Long goodsId;
    private String goodsName;
    private String goodsImg;
    private String goodsDetail;
    private BigDecimal goodsPrice;
    private BigDecimal seckillPrice;
    private Integer stockCount;
    private Integer limitCount;
    private Long merchantId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
}