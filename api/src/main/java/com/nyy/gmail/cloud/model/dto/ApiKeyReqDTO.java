package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApiKeyReqDTO {
    private String id;
    private String whiteIp;
    private List<String> ids;
    private String receiveCallbackUrl;
    private String userID;
}
