package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatroomMessageReqDto {
    private String accId;
    private String friendId;
    private String startMessageId; // 向前翻消息

    private String endMessageId; // 向后翻消息

    private List<String> ids;
}
