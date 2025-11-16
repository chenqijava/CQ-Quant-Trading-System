package com.nyy.gmail.cloud.enums;

public enum BindStatusEnums {

    UNBIND("0","未分配"),BINDING("1","已分配");

    private String status;

    private String desc;

    BindStatusEnums(String status, String desc) {
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
