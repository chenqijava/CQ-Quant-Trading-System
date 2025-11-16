package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ChatroomAccountRespDto {

    private List<AccountGroupDto> groupDtoList;

    @Data
    public static class AccountGroupDto {
        private List<AccountDto> accountDtoList;
        private String _id;
        private String groupName;
    }

    @Data
    public static class AccountDto {
        private String onlineStatus;
        private String phone;
        private Long unreadMessageCount;
        private Date lastMessageTime;
        private String lastMessage;
        private String _id;
        private String remark;
    }
}
