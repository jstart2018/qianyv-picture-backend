package com.jstart.qianyvpicturebackend.model.dto.spaceUser;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 空间用户关联
 * @TableName space_user
 */
@TableName(value ="space_user")
@Data
public class SpaceUserEditRequest implements Serializable {

    /**
     * 空间成员id
     */
    Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;


    private static final long serialVersionUID = 1L;
}