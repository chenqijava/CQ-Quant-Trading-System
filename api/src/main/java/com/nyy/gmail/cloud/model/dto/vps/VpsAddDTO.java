package com.nyy.gmail.cloud.model.dto.vps;

import lombok.Data;

import java.io.Serializable;

@Data
public class VpsAddDTO implements Serializable {

    private Integer addNum;

    private String userId;
}
