package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class MessageReqDTO {

    private String accid;

    private String uid;

    private String fromId;

    private Integer page = 1;

    private Integer pageSize = 20;
}
