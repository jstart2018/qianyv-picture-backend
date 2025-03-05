package com.jstart.qianyvpicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间请求
 */
@Data
public class SpaceAddRequest implements Serializable {

    private static final long serialVersionUID = -2563043962777854717L;

    //空间名称
    private String spaceName;
    //空间等级
    private Integer spaceLevel;

    /**
     * 空间类型： 0私有 ，1 团队
     */
    private Integer spaceType;



}
