package com.jstart.qianyvpicturebackend.common;

import com.jstart.qianyvpicturebackend.exception.ErrorEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorEnum errorEnum) {
        this(errorEnum.getCode(), null, errorEnum.getMessage());
    }
}

