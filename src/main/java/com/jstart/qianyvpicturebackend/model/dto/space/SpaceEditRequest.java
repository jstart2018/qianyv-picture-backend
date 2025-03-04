package com.jstart.qianyvpicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户编辑请求
 */
@Data
public class SpaceEditRequest implements Serializable {

    private static final long serialVersionUID = 2958041504149997183L;

    //空间id
    private Long id;
    //空间名称
    private String spaceName;



}
