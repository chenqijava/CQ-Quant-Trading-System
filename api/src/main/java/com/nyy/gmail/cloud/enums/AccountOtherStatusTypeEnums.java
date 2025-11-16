package com.nyy.gmail.cloud.enums;

/**
 * 账号其他状态枚举
 */
public enum AccountOtherStatusTypeEnums {
    DEFAULT("scanQrCodeFailOrTimeout", "未扫码成功或超时"),
    NORMAL("normal", "正常"),
    SCAN_QR_CODE_FAIL_OR_TIMEOUT("scanQrCodeFailOrTimeout", "未扫码成功或超时"),
    DEVICE_EXPIRED("deviceExpired", "设备过期"),
    NOT_ENOUGH_IP("notEnoughIp", "无可用IP"),
    LOGIN_FAIL("loginFail", "登录失效"),
    // 相同账号 -》 账号已被使用
    SAME_ACCOUNT("sameAccount", "账号已被使用"),
    ACCOUNT_ABNORMALITY("accountAbnormality", "账号异常"),
    ;

    private final String code;
    private final String description;
    private final int priority;

    AccountOtherStatusTypeEnums(String code, String description) {
        this.code = code;
        this.description = description;
        this.priority = 0;
    }

    AccountOtherStatusTypeEnums(String code, String description, int priority) {
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


    public static AccountOtherStatusTypeEnums fromCode(String code) {
        for (AccountOtherStatusTypeEnums taskType : values()) {
            if (taskType.code.equals(code)) {
                return taskType;
            }
        }
        return null;
    }
}
