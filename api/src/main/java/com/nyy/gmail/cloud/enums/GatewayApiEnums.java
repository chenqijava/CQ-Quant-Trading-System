package com.nyy.gmail.cloud.enums;

public enum GatewayApiEnums {
    ACCOUNT_AUTH("/account/auth", "google账号授权gmail"),
    MAKE_SESSION("/common/makeSession", "登录gmail"),
    GET_INBOX_EMAIL_LIST("/gmail/getInboxEmailList", "获取邮件列表"),
    GET_EMAIL_DETAIL("/gmail/getEmailDetail", "获取邮件详情"),
    SEND_EMAIL("/gmail/sendEmail", "发送邮件"),
    REMOVE_FILTERS("/gmail/removeFilters", "删除过滤器")
    ;

    private final String code;
    private final String description;

    GatewayApiEnums(String code, String description) {
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
    public static GatewayApiEnums fromCode(String code) {
        for (GatewayApiEnums status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
