package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class QueryPlatformCountReqDto {

    private String platformId;

    private List<String> ids;

}
