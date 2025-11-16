package com.nyy.gmail.cloud.model.dto.vps;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购买设备
 */
@Data
public class VpsBuyDTO implements Serializable {


    /**
     * 购买数量
     */
    private Integer amount;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 购买时长，月数
     */
    private Integer monthCount;


    private String userId;


}
