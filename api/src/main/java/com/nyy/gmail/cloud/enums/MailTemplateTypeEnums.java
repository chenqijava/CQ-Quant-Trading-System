package com.nyy.gmail.cloud.enums;

public enum MailTemplateTypeEnums {
    SYSTEM(0, "系统"),
    USER (1, "个人"),
    ;

    private final Integer code;
    private final String description;

    MailTemplateTypeEnums(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MailTemplateTypeEnums fromCode(Integer code) {
        for (MailTemplateTypeEnums status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
