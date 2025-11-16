package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AccountListDTO {

    private List<String> ids;

    private Map<String, Object> filters;

    private Map<String, Integer> sorter;
}
