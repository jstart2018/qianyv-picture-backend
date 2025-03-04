package com.jstart.qianyvpicturebackend.common.enums;

import lombok.Getter;

@Getter
public enum SpaceLevelEnum {
    COMMON(1,"普通版",100L*1024*1024,100),
    PROFESSIONAL(2,"专业版",1024L*1024*1024,1000),
    FLAGSHIP(3,"旗舰版", 10L *1024*1024*1024,10000);


    private final int value;
    private final String desc;
    private final long maxSize;
    private final long maxCount;


    private SpaceLevelEnum(int value, String desc, long maxSize, long maxCount) {
        this.value = value;
        this.desc = desc;
        this.maxSize = maxSize;
        this.maxCount = maxCount;
    }


    public static SpaceLevelEnum getEnumByValue(int value) {
        for (SpaceLevelEnum spaceLevelEnum : values()) {
            if (spaceLevelEnum.getValue() == value) {
                return spaceLevelEnum;
            }
        }
        return null;
    }



}
