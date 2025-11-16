package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class LinkCheckReqDto {

    private String desc;

    private String addMethod="1";

    private String addData;

    private String filepath;

    private Integer sendNum = 10;
}
