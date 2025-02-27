package com.jstart.qianyvpicturebackend.common.enums;

import lombok.Getter;

@Getter
public enum PictureStatusEnum {


    REVIEWING("待审核",0),
    PASS("通过",1),
    REJECT("拒绝",2);


    private final String desc;
    private final Integer value;

    PictureStatusEnum(String desc, Integer value) {
        this.desc = desc;
        this.value = value;
    }

    public static PictureStatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (PictureStatusEnum pictureStatusEnum : PictureStatusEnum.values()) {
            if (pictureStatusEnum.getValue().equals(value)) {
                return pictureStatusEnum;
            }
        }
        return null;
    }



}
