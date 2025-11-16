package com.nyy.gmail.cloud.enums;

public enum AccountTypeEnums {
    mobile("mobile", "mobile"),
    web("web", "web"),
    sendgrid("sendgrid", "sendgrid"),
    outlook_graph("outlook_graph", "outlook graph"),
    workspace_service_account("workspace_service_account", "workspace service account"),
    workspace_second_hand_account("workspace_second_hand_account", "workspace second hand account"),
    yahoo("yahoo", "yahoo"),
    smtp("smtp", "smtp"),
    ;

    private final String code;
    private final String description;

    AccountTypeEnums(String code, String description) {
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
    public static AccountTypeEnums fromCode(String code) {
        for (AccountTypeEnums status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
