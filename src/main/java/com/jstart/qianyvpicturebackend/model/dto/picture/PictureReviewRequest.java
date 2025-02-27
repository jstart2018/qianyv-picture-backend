package com.jstart.qianyvpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureReviewRequest implements Serializable {

    //图片id
    private Long id;

    //审核状态
    private int reviewStatus;

    //审核回显信息
    private String reviewMessage;


}
