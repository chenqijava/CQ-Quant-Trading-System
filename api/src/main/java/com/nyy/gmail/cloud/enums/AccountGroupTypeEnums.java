package com.nyy.gmail.cloud.enums;

public enum AccountGroupTypeEnums {
    NORMAL("normal", "普通分组"),
    DEFAULT("default", "默认分组"),
    ;

    private final String code;
    private final String description;
    private final int priority;

    AccountGroupTypeEnums(String code, String description) {
        this.code = code;
        this.description = description;
        this.priority = 0;
    }

    AccountGroupTypeEnums(String code, String description, int priority) {
        this.code = code;
        this.description = description;
        this.priority = priority;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }


    public static AccountGroupTypeEnums fromCode(String code) {
        for (AccountGroupTypeEnums taskType : values()) {
            if (taskType.code.equals(code)) {
                return taskType;
            }
        }
        return null;
    }
}
