package com.jstart.qianyvpicturebackend.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;


    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultEnum resultEnum) {
        super(resultEnum.getMessage());
        this.code = resultEnum.getCode();
    }

    public BusinessException(ResultEnum resultEnum, String message) {
        super(message);
        this.code = resultEnum.getCode();
    }


}
