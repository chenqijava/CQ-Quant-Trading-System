package com.nyy.gmail.cloud.gateway.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GatewayResultBase implements Serializable {
    private static final long serialVersionUID = -8119786652052900616L;

    private Integer code;

    private String msg;

}
