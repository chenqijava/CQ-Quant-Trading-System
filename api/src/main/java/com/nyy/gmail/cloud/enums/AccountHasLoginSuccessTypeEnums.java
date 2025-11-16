package com.nyy.gmail.cloud.enums;

public enum AccountHasLoginSuccessTypeEnums {
    NO("no", "no"),
    YES("yes", "yes"),
    ;

    private final String code;
    private final String description;
    private final int priority;

    AccountHasLoginSuccessTypeEnums(String code, String description) {
        this.code = code;
        this.description = description;
        this.priority = 0;
    }

    AccountHasLoginSuccessTypeEnums(String code, String description, int priority) {
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


    public static AccountHasLoginSuccessTypeEnums fromCode(String code) {
        for (AccountHasLoginSuccessTypeEnums taskType : values()) {
            if (taskType.code.equals(code)) {
                return taskType;
            }
        }
        return null;
    }
}
