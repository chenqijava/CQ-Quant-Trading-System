package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class ChatroomSendMessageReqDto {

    private String accId;

    private String friendId;

    private String content;

    private String filepath;

}
