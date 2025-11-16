package com.nyy.gmail.cloud.model.vo;

import lombok.Data;

import java.time.Instant;

@Data
public class VpsInfoVO {

    private Long id;

    private String vpsId;


    private String userId;


    private String accid;


    private String loginStatus;


    private String runStatus;

    private Instant deadTime;

    private String bindType;


    private String bindStatus;

    private Integer renewTimes;

    private String description;

}