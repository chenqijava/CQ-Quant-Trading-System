package com.nyy.gmail.cloud.gateway.dto;

import lombok.Data;

@Data
public class MakeSessionResponse extends GatewayResultBase {

    private Profile profile;

    private String session;

    private String deviceinfo;
}
