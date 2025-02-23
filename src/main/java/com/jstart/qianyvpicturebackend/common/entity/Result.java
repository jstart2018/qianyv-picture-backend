package com.jstart.qianyvpicturebackend.common.entity;

import com.jstart.qianyvpicturebackend.exception.ErrorEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    private Result(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>(ErrorEnum.SUCCESS.getCode(),
                data, ErrorEnum.SUCCESS.getMessage());

    }

    public static <T> Result<T> success(T data,String message) {
        return new Result<T>(ErrorEnum.SUCCESS.getCode(),
                data, message);

    }

    public static  Result success() {

        return new Result(ErrorEnum.SUCCESS.getCode(),
                null, ErrorEnum.SUCCESS.getMessage());
    }

    public static <T> Result<T> error(ErrorEnum errorEnum) {
        return new Result<>(errorEnum.getCode(),null,errorEnum.getMessage());
    }

    public static <T> Result<T> error(ErrorEnum errorEnum,String message) {
        return new Result<>(errorEnum.getCode(),null,message);
    }

    public static <T> Result<T> error(int code,String message) {
        return new Result<>(code,null,message);
    }


}

