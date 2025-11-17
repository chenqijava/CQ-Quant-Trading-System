package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class VpsRespDTO {

    private Long total;

    private String unitPrice;

    private BigDecimal balance;

    private Map<String, Integer> userBindCount;

    private Map<String, Integer> runStatusCount;

    private Map<String, Integer> deadStatusCount;

    private Map<String, Integer> bindStatusCount;
}
