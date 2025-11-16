package com.nyy.gmail.cloud.enums;

import lombok.Data;

public enum MailTemplateStatusEnums {
    NORMAL(0, "可用"),
    FROZEN (1, "冻结"),
    ;

    private final Integer code;
    private final String description;

    MailTemplateStatusEnums(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MailTemplateStatusEnums fromCode(Integer code) {
        for (MailTemplateStatusEnums status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
