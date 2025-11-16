package com.nyy.gmail.cloud.enums;

public enum IpStatusEnum {
    INIT("init", "初始化"),

    ;


    private final String code;
    private final String description;

    IpStatusEnum(String code, String description) {
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
