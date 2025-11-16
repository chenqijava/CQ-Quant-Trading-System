package com.nyy.gmail.cloud.common.exception;

import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoLoginException.class)
    @ResponseBody
    public Result<Void> resolveNoLoginException(NoLoginException noLoginException) {
        log.error(noLoginException.getMessage());
        return ResponseResult.failure(noLoginException.getCode(), noLoginException.getMsg());
    }

    @ExceptionHandler(CommonException.class)
    @ResponseBody
    public Result<Void> resolveCommonException(CommonException commonException) {
        log.error("", commonException);
        return ResponseResult.failure(commonException.getCode(), commonException.getMsg());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public Result<Void> resolveDuplicateKeyException(DuplicateKeyException duplicateKeyException) {
        log.error(duplicateKeyException.getMessage());
        return ResponseResult.failure(ResultCode.DATA_IS_EXISTED);
    }

    /**
     * 校验参数异常处理器
     * @param ex
     * @return
     */
    @ResponseBody
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> bindException(IllegalStateException ex) {
        System.out.println(ex);
        return ResponseResult.failure(ResultCode.ERROR);
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> validationError(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        log.error(fieldError.getField() + fieldError.getDefaultMessage());
        return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID.getCode(), fieldError.getField() + fieldError.getDefaultMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result<Void> resolveException(Exception exception) {
        log.error("服务器内部错误", exception);
        return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(NotifyException.class)
    public ResponseEntity<String> NotifyException(NotifyException ex) {
        return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
