package com.jstart.qianyvpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户
 * @TableName user
 */
@Data
public class UserRegisterDTO implements Serializable {
    private static final long serialVersionUID = -7283818918621798301L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 再次确认密码
     */
    private String checkPassword;




}