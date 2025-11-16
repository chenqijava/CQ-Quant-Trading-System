package com.nyy.gmail.cloud.utils.tgnet;

public class Address {
    private String addr;
    private int port;
    private int flag;
    private byte[] secret;

    public Address(String addr, int port, int flag, byte[] secret) {
        this.addr = addr;
        this.port = port;
        this.flag = flag;
        this.secret = secret;
    }

    // Getter方法
    public String getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public int getFlag() {
        return flag;
    }

    public byte[] getSecret() {
        return secret;
    }

    // Setter方法
    public void setAddr(String addr) {
        this.addr = addr;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public void setSecret(byte[] secret) {
        this.secret = secret;
    }
}

