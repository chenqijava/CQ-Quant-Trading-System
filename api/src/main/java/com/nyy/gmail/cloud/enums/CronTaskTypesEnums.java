package com.nyy.gmail.cloud.enums;

public enum CronTaskTypesEnums {
    CheckAccountTCPConnection("CheckAccountTCPConnection", "心跳检测"),
    // 其他任务...
    AccountCheckLogin("AccountCheckLogin", "检查账号是否扫码成功"),
    ;

    private final String code;
    private final String description;
    private final int priority;

    CronTaskTypesEnums(String code, String description) {
        this.code = code;
        this.description = description;
        this.priority = 0;
    }

    CronTaskTypesEnums(String code, String description, int priority) {
        this.code = code;
        this.description = description;
        this.priority = priority;
    }

    /**
     * 获取任务类型的代码
     * @return 任务类型代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取任务类型的描述信息
     * @return 任务类型描述
     */
    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 根据代码查找对应的任务类型枚举
     * @param code 任务类型代码
     * @return 对应的任务类型枚举，如果未找到则返回 null
     */
    public static CronTaskTypesEnums fromCode(String code) {
        for (CronTaskTypesEnums taskType : values()) {
            if (taskType.code.equals(code)) {
                return taskType;
            }
        }
        return null;
    }
}
