package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExportAccountReqDto {

    private String platformId;

    private Integer count;

    private List<String> ids;

    private String exportType;

    private String orderId;
}
