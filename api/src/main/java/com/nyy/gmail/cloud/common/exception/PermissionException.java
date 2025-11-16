package com.nyy.gmail.cloud.common.exception;

import com.nyy.gmail.cloud.common.response.ResultCode;

public class PermissionException extends CommonException {
    public PermissionException() {
        super(ResultCode.METHOD_NOT_ALLOWED);
    }
}
