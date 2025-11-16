package com.nyy.gmail.cloud.enums;

public enum MessageStatusEnums {
    正在发送中("正在发送中", "正在发送中"),
    发送成功("发送成功", "发送成功"),
    发送失败("发送失败", "发送失败"),
    ;
    private final String code;
    private final String description;

    MessageStatusEnums(String code, String description) {
        this.code = code;
        this.description = description;
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

    /**
     * 根据代码查找对应的任务类型枚举
     * @param code 任务类型代码
     * @return 对应的任务类型枚举，如果未找到则返回 null
     */
    public static MessageStatusEnums fromCode(String code) {
        for (MessageStatusEnums taskType : values()) {
            if (taskType.code.equals(code)) {
                return taskType;
            }
        }
        return null;
    }
}
