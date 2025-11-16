package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserChargeVpsReqDTO {

    private List<String> ids;

    private List<String> deadTimes;

    private Integer startMachine;

    private Integer endMachine;

    private Integer chargeMonthValue;

}
