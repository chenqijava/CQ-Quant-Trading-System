package com.nyy.gmail.cloud.common.enums;

public enum Socks5TypeEnums {
    DynamicProxy(1),
    StaticProxy(2),
    All(3);

    Socks5TypeEnums(Integer value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    private Integer value;
}
