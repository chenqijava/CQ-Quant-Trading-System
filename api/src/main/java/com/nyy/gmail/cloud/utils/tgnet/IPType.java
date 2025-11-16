package com.nyy.gmail.cloud.utils.tgnet;

public enum IPType {
    IPv4(1),
    IPv6(2),
    DOWNLOAD_IPv4(3),
    DOWNLOAD_IPv6(4);

    private final int value;

    IPType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

