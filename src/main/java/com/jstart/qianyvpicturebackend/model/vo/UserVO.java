package com.jstart.qianyvpicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户
 * @TableName user
 */
@Data
public class UserVO implements Serializable {
    private static final long serialVersionUID = -7283818918621798301L;
    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;



}