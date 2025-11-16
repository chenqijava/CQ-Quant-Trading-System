package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class GroupAccountStats {
    private String id;          // groupID
    private long total;         // 总账号数
    private long onlineTotal;   // 在线账号数
}
