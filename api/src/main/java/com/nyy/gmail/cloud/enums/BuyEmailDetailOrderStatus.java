package com.nyy.gmail.cloud.enums;


public enum BuyEmailDetailOrderStatus {
    CodeReceived("CodeReceived", "已接码"),
    Released("Released", "已释放"),
    Expired("Expired", "已过期"),
    Waiting("Waiting", "待接码"),
    ;

    private final String code;
    private final String description;

    BuyEmailDetailOrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取账号状态对应的代码
     * @return 状态代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取账号状态的描述信息
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据代码查找对应的账号状态枚举
     * @param code 状态代码
     * @return 对应的账号状态枚举，如果未找到则返回 null
     */
    public static BuyEmailDetailOrderStatus fromCode(String code) {
        for (BuyEmailDetailOrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}