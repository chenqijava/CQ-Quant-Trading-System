package com.nyy.gmail.cloud.tasks.sieve.enums;


public enum SieveActiveTaskResultTypeEnum {
    success("成功"),
    failed("失败"),
    unexecute("未同步"),
    forbidden("禁用"),
    unknown("未知");

    private String name;

    SieveActiveTaskResultTypeEnum(String name) {
        this.name = name;
    }

}
