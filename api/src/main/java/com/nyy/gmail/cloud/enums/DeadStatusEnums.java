package com.nyy.gmail.cloud.enums;

public enum DeadStatusEnums {

    VALLID("0","有效"),TO_EXPIRE("1","即将过期"),EXPIRED("2","过期");

    private String status;

    private String desc;


    DeadStatusEnums(String status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public String getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}
