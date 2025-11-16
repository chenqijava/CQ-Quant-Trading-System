package com.nyy.gmail.cloud.enums;

public enum WalletTypeEnums {

    USDT_TRC20("TRC20","USDT_TRC20"),;

    private final String code;
    private final String description;

    WalletTypeEnums(String code, String description) {
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
