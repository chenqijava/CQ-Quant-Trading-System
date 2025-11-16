package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class SecondSendRespDto {

    private String id;

    private String name;

    private Integer open;

    private Integer noOpen;

    private Integer reply;

    private Integer noReply;

    private Integer click;

    private Integer noClick;

}
