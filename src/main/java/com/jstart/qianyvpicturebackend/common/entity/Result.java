package com.jstart.qianyvpicturebackend.common.entity;

import com.jstart.qianyvpicturebackend.exception.ResultEnum;
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
        return new Result<T>(ResultEnum.SUCCESS.getCode(),
                data, ResultEnum.SUCCESS.getMessage());

    }

    public static <T> Result<T> success(T data,String message) {
        return new Result<T>(ResultEnum.SUCCESS.getCode(),
                data, message);

    }

    public static  Result success() {

        return new Result(ResultEnum.SUCCESS.getCode(),
                null, ResultEnum.SUCCESS.getMessage());
    }

    public static <T> Result<T> error(ResultEnum resultEnum) {
        return new Result<>(resultEnum.getCode(),null, resultEnum.getMessage());
    }

    public static <T> Result<T> error(ResultEnum resultEnum, String message) {
        return new Result<>(resultEnum.getCode(),null,message);
    }

    public static <T> Result<T> error(int code,String message) {
        return new Result<>(code,null,message);
    }


}

