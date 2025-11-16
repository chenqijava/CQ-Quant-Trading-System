package com.nyy.gmail.cloud.model.dto;

import com.nyy.gmail.cloud.entity.mongo.Vps;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class VpsRespDTO {

    private List<Vps> data;

    private Long total;

    private String unitPrice;

    private BigDecimal balance;

    private Map<String, Integer> userBindCount;

    private Map<String, Integer> runStatusCount;

    private Map<String, Integer> deadStatusCount;

    private Map<String, Integer> bindStatusCount;
}
