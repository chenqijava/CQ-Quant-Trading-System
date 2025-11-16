package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminDelayVpsReqDTO {

    private List<String> ids;

    private List<String> deadTimes;

    private Integer startMachine;

    private Integer endMachine;

    private String chargeDelayValue;
}
