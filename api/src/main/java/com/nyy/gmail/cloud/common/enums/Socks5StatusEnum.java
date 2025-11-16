package com.nyy.gmail.cloud.common.enums;

public enum Socks5StatusEnum {
    OK(1),
    NETERROR(2);

    Socks5StatusEnum(Integer value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    private Integer value;
}
