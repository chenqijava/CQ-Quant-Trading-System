package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlatformPriceRespDto {

    private String userID;

    private String platformId;

    private BigDecimal price;

    private BigDecimal defaultPrice;

    private String platformName;
}
