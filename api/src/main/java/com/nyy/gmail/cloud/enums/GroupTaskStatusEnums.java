package com.nyy.gmail.cloud.enums;

public enum GroupTaskStatusEnums {
//    waitPublish -> processing -> init -> pause/success/failed
    waitPublish("waitPublish", "待发布"),
    processing("processing", "处理中"),
    init("init", "待执行"),
    pause("pause", "暂停"),
    success("success", "完成"),
    failed("failed", "失败"),
    ;

    private final String code;
    private final String description;

    GroupTaskStatusEnums(String code, String description) {
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
    public static GroupTaskStatusEnums fromCode(String code) {
        for (GroupTaskStatusEnums status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
