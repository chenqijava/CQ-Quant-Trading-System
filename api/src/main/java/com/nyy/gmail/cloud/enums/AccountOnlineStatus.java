package com.nyy.gmail.cloud.enums;

public enum AccountOnlineStatus {
    // 被封，代码为 -1
    BANNED("-1", "被封"),
    // 离线，代码为 0
    OFFLINE("0", "离线"),
    // 上线，代码为 1
    ONLINE("1", "上线"),
    // 等待上线，代码为 2
    WAITING_ONLINE("2", "等待上线"),
    ;

    private final String code;
    private final String description;

    AccountOnlineStatus(String code, String description) {
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
    public static AccountOnlineStatus fromCode(String code) {
        for (AccountOnlineStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
