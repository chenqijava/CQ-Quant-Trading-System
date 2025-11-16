package com.nyy.gmail.cloud.enums;

public enum RunStatusEnums {

    STOP("0","掉线"),RUNNING("1","运行中");

    private String status;

    private String desc;

    RunStatusEnums(String status, String desc) {
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
