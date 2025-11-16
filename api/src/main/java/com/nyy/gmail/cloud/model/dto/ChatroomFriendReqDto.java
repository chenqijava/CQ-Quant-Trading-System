package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatroomFriendReqDto {
    private String searchKey;
    private List<String> ids;
}
