package com.nyy.gmail.cloud.common.enums;

import java.util.HashMap;
import java.util.Map;

public enum ProjectEnums {
    EMAIL("email"),
    FB("fb"),
    APPLE("apple"),
    AMAZON("amazon"),
    ;

    ProjectEnums(String value) {
        this.value = value;
    }

    private String value;

    private static final Map<String, ProjectEnums> enumByValue = new HashMap<>();

    static {
        for (ProjectEnums enumValue : ProjectEnums.values()) {
            enumByValue.put(enumValue.getValue(), enumValue);
        }
    }

    public static ProjectEnums getByValue(String value) {
        return enumByValue.get(value);
    }

    public static boolean hasValue(String value) {
        return enumByValue.containsKey(value);
    }

    public String getValue() {
        return value;
    }
}
