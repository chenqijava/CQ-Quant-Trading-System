package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AccountPlatformReqDto {

    private Map<String, Object> filters;

    private Map<String, Integer> sorter;
}
