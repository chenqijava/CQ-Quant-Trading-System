package com.nyy.gmail.cloud.common.response;

import com.nyy.gmail.cloud.common.exception.CommonException;

public class ResponseResult {
    /**
     * 只返回状态
     * @return Result
     */
    public static Result<Void> success() {
        return new Result<Void>().setResult(ResultCode.SUCCESS);
    }

    /**
     * 成功返回数据
     * @param data
     * @return Result
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<T>().setResult(ResultCode.SUCCESS, message, data);
    }

    /**
     * 成功返回数据
     * @param data
     * @return Result
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>().setResult(ResultCode.SUCCESS, data);
    }

    /**
     * 失败
     * @param resultCode
     * @return Result
     */
    public static <T> Result<T> failure(ResultCode resultCode) {
        return new Result<T>().setResult(resultCode);
    }

    /**
     * 失败
     * @param resultCode
     * @param data
     * @return Result
     */
    public static <T> Result<T> failure(ResultCode resultCode, String message) {
        return new Result<T>().setResult(resultCode, message, null);
    }

    public static <T> Result<T> failure(ResultCode resultCode, String message, T data) {
      return new Result<T>().setResult(resultCode, data);
  }

    /**
     * 失败
     * @param resultCode
     * @param data
     * @return Result
     */
    public static <T> Result failure(ResultCode resultCode, T data) {
        return new Result()
                .setResult(resultCode, data);
    }

    /**
     * 失败
     * @param code
     * @param message
     * @return
     */
    public static <T> Result<T> failure(Integer code, String message) {
        return new Result<T>().setResult(code, message, null);
    }

    public static <T> Result<T> failure(Integer code, String message, T data) {
        return new Result<T>().setResult(code, message, data);
    }

    public static <T> Result<T> failureWithEnglish(Integer code, String message, String englishMessage, T data) {
        return new Result<T>().setResult(code, message, englishMessage, data);
    }

    /**
     * 失败
     * @param ex
     * @return
     */
    public static <T> Result<T> failure(CommonException ex) {
        return new Result<T>().setResult(ex.getCode(), ex.getMessage(), null);
    }
}
