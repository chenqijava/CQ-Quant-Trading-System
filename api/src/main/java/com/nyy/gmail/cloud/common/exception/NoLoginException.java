package com.nyy.gmail.cloud.common.exception;

import com.nyy.gmail.cloud.common.response.ResultCode;

public class NoLoginException extends CommonException {
    public NoLoginException() {
        super(ResultCode.UNAUTHORIZED);
    }

    public NoLoginException(String msg) {
        super(ResultCode.UNAUTHORIZED, msg);
    }
}
