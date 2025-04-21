package com.jstart.qianyvpicturebackend.exception;


import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.jstart.qianyvpicturebackend.common.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return Result.error(e.getCode(), e.getMessage());
    }
    //Preconditions参数校验产生的异常
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> businessExceptionHandler(IllegalArgumentException e) {
        log.error("IllegalArgumentException", e);
        return Result.error(ResultEnum.PARAMS_ERROR, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return Result.error(ResultEnum.SYSTEM_ERROR);
    }

    @ExceptionHandler(NotLoginException.class)
    public Result<?> notLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return Result.error(ResultEnum.NOT_LOGIN_ERROR, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException", e);
        return Result.error(ResultEnum.NO_AUTH_ERROR, e.getMessage());
    }


}
