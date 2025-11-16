package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SaveQuickReplyReqDto {

    private String type;
    private Map params;
    private String userID;
    private String quickCommand;
    private Integer sortNo = 0;
    private String id;
    private String name;
}
