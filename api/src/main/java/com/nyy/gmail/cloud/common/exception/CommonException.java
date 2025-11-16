package com.nyy.gmail.cloud.common.exception;

import com.nyy.gmail.cloud.common.response.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.nyy.gmail.cloud.common.response.ResultCode.ERROR;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommonException extends RuntimeException {
    private int code;
    private String msg;

    public CommonException(String msg) {
        this(ERROR, msg);
    }

    public CommonException(ResultCode code) {
        super(code.getMessage());
        this.code = code.getCode();
        this.msg = code.getMessage();
    }

    public CommonException(ResultCode code, String msg) {
        super(msg);
        this.code = code.getCode();
        this.msg = msg;
    }
}
