package com.nyy.gmail.cloud.enums;

import lombok.Data;
import lombok.Setter;

public enum UserStatusEnum {
    // 允许使用
    ENABLE("enable", "允许使用"),
    // 被封
    DISABLED("disabled", "冻结"),
    ;


    private final String code;
    private final String description;

    UserStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }


    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
