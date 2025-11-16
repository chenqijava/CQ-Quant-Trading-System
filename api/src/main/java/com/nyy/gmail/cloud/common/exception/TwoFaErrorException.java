package com.nyy.gmail.cloud.common.exception;

import com.nyy.gmail.cloud.common.response.ResultCode;

public class TwoFaErrorException extends CommonException {
    public TwoFaErrorException() {
        super(ResultCode.TWO_FA_ERROR);
    }

    public TwoFaErrorException(String msg) {
        super(ResultCode.TWO_FA_ERROR, msg);
    }
}
