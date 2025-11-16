package com.nyy.gmail.cloud.common.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = -4167217748594806440L;

    private Integer code;

    private String message;

    private String englishMessage;

    private T data;

    public Result<T> setResult(ResultCode resultCode) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
        return this;
    }

    public Result<T> setResult(ResultCode resultCode, String message, T data) {
        this.code = resultCode.getCode();
        this.message = message;
        this.setData(data);
        return this;
    }

    public Result<T> setResult(ResultCode resultCode, T data) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
        this.setData(data);
        return this;
    }

    public Result<T> setResult(Integer code, String message) {
        this.code = code;
        this.message = message;
        return this;
    }

    public Result<T> setResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.setData(data);
        return this;
    }

    public Result<T> setResult(Integer code, String message, String englishMessage, T data) {
        this.code = code;
        this.message = message;
        this.englishMessage = englishMessage;
        this.setData(data);
        return this;
    }
}
