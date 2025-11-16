package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ChargeReqTDO {

    private BigDecimal chargeValue;

    private List<String> ids;

}
