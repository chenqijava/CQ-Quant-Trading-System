package com.nyy.gmail.cloud.common.enums;

public enum AccountRunTaskStatusEnum {
    // 空闲中
    READY("ready"),
    // 检测任务中
    CHECKING("checking"),
    // 等待执行任务中
    WAITING("waiting"),
    // 正在执行
    RUNNING("running");

    private final String code;

    AccountRunTaskStatusEnum(String code) {
        this.code = code;
    }

    /**
     * 获取枚举对应的状态码
     * @return 状态码
     */
    public String getCode() {
        return code;
    }

    /**
     * 根据状态码查找对应的枚举值
     * @param code 状态码
     * @return 对应的枚举值，如果未找到则返回 null
     */
    public static AccountRunTaskStatusEnum fromCode(String code) {
        for (AccountRunTaskStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}