package com.nyy.gmail.cloud.enums;

public enum BillExpenseTypeEnums {
    IN("1", "收入"),
    OUT("2", "支出"),
    ;

    private final String code;
    private final String description;
    private final int priority;

    BillExpenseTypeEnums(String code, String description) {
        this.code = code;
        this.description = description;
        this.priority = 0;
    }

    BillExpenseTypeEnums(String code, String description, int priority) {
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
    public static BillExpenseTypeEnums fromCode(String code) {
        for (BillExpenseTypeEnums taskType : values()) {
            if (taskType.code.equals(code)) {
                return taskType;
            }
        }
        return null;
    }
}
