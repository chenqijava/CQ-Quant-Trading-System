package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class AccountGroupListDTO implements Serializable {

    private List<String> ids;

    private Map<String, Object> filters;

    private Map<String, Integer> sorter;

    private boolean includeAccountNum = false;
}
