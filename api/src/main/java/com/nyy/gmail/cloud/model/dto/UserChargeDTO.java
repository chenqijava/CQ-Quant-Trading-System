package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 用户充值DTO
 */

@Data
@ToString
@Accessors(chain = true)
public class UserChargeDTO implements Serializable {

    /**
     * 待充值金额
     */
    private BigDecimal chargeValue;

    /**
     * 待充值的用户ids
     */
    private List<String> ids;
}
