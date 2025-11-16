package com.nyy.gmail.cloud.enums;

public enum PaymentStatusEnums {
    init("init", "待支付"),
    processing("processing", "支付中"),
    success("success", "成功"),
    failed("failed", "失败");

    private final String code;
    private final String description;

    PaymentStatusEnums(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
