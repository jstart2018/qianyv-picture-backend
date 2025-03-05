package com.jstart.qianyvpicturebackend.common.enums;

import lombok.Getter;

@Getter
public enum SpaceRoleEnum {

    VIEWER("viewer","查看者"),
    EDITOR("editor","编辑者"),
    ADMIN("ADMIN","管理员");

    private String value;
    private String desc;

    SpaceRoleEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }


    public static SpaceRoleEnum getByValue(String value) {
        for (SpaceRoleEnum spaceRoleEnum : SpaceRoleEnum.values()) {
            if (spaceRoleEnum.value.equals(value)) {
                return spaceRoleEnum;
            }
        }
        return null;
    }


}
