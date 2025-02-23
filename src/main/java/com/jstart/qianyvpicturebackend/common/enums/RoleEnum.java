package com.jstart.qianyvpicturebackend.common.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {

    USER("user","用户"),
    ADMIN("admin","管理员");


    private final String value;
    private final String msg;

    RoleEnum(String value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    public static RoleEnum getByValue(String value) {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            if (roleEnum.getValue().equals(value)) {
                return roleEnum;
            }
        }
        return null;
    }


}
