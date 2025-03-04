package com.jstart.qianyvpicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员更新请求
 */
@Data
public class SpaceUpdateRequest implements Serializable {
    private static final long serialVersionUID = 360180947098637068L;

    //空间id
    private Long id;
    //空间名称
    private String spaceName;
    //空间等级
    private int spaceLevel;
    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;


}
