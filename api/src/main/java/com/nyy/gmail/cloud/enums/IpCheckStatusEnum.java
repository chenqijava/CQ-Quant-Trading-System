package com.nyy.gmail.cloud.enums;

public enum IpCheckStatusEnum {
    CHECKED_SUCCESS("yes", "yes"),
    CHECKED_FAIL("no", "no"),
    CHECKING("checking", "checking"),
    ;


    private final String code;
    private final String description;

    IpCheckStatusEnum(String code, String description) {
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
