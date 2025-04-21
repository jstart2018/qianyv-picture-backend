package com.jstart.qianyvpicturebackend.exception;

import lombok.Getter;

@Getter
public class ThrowUtils {

    /**
     *  条件成立则抛出异常
     * @param condition 条件
     * @param exception 异常
     */
    public static void throwIf(boolean condition,RuntimeException exception) {
        if (condition) {
            throw exception;
        }
    }
    /**
     *  条件成立则抛出异常
     * @param condition 条件
     * @param exceptionCode 异常code
     */
    public static void throwIf(boolean condition, ResultEnum exceptionCode) {
        if (condition) {
            throwIf(condition,new BusinessException(exceptionCode));
        }
    }
    /**
     *  条件成立则抛出异常
     * @param condition 条件
     * @param exceptionCode 异常code
     * @param message   异常消息
     */
    public static void throwIf(boolean condition, ResultEnum exceptionCode, String message) {
        if (condition) {
            throwIf(condition,new BusinessException(exceptionCode,message));
        }
    }

}
