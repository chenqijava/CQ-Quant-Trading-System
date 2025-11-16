package com.nyy.gmail.cloud.model.dto.vps;

import lombok.Data;

import java.util.Map;

@Data
public class VpsQueryDTO {


    private Map<String, Object> filters;

    private String userId;

    private Integer pageSize;

    private Integer pageNo;

    private Boolean userAdmin;
}
