package com.nyy.gmail.cloud.enums;

public enum BillCateTypeEnums {
    CHARGE_SEND_EMAIL_COUNT("charge_send_email_count", "充值发送邮件次数"),
    DEDUCT_SEND_EMAIL_COUNT("deduct_send_email_count", "扣减发送邮件次数"),
    RESTORE_SEND_EMAIL_COUNT("restore_send_email_count", "恢复发送邮件次数"),
    CHARGE_OFFLINE("charge_offline", "线下充值"),
    CHARGE_ONLINE("charge_online", "在线充值"),
    BUY_VPS("buyVps", "购买设备"),
    CHARGE_VPS("chargeVps", "设备续费"),
    DELAY_VPS("delayVps", "设备延期"),
    BUY_ORDER("buyOrder", "下单扣款"),
    MANUAL_DEDUCTION("manualDeduction", "人工扣款"),
    COMMISSION("Commission", "返佣"),
    CONSUMPTION("Consumption", "消费"),
    REFUND("Refund", "退款"),
    ;

    private final String code;
    private final String description;
    private final int priority;

    BillCateTypeEnums(String code, String description) {
        this.code = code;
        this.description = description;
        this.priority = 0;
    }

    BillCateTypeEnums(String code, String description, int priority) {
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
    public static BillCateTypeEnums fromCode(String code) {
        for (BillCateTypeEnums taskType : values()) {
            if (taskType.code.equals(code)) {
                return taskType;
            }
        }
        return null;
    }
}
