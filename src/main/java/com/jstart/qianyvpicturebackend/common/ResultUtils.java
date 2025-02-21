package com.jstart.qianyvpicturebackend.common;

import com.jstart.qianyvpicturebackend.exception.ErrorEnum;

public class ResultUtils {

    /**
     * 成功
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败
     *
     * @param errorEnum 错误码
     * @return 响应
     */
    public static BaseResponse<?> error(ErrorEnum errorEnum) {
        return new BaseResponse<>(errorEnum);
    }

    /**
     * 失败
     *
     * @param code    错误码
     * @param message 错误信息
     * @return 响应
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败
     *
     * @param errorEnum 错误码
     * @return 响应
     */
    public static BaseResponse<?> error(ErrorEnum errorEnum, String message) {
        return new BaseResponse<>(errorEnum.getCode(), null, message);
    }
}
