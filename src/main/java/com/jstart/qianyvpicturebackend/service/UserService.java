package com.jstart.qianyvpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jstart.qianyvpicturebackend.model.dto.user.UserLoginDTO;
import com.jstart.qianyvpicturebackend.model.dto.user.UserQueryRequest;
import com.jstart.qianyvpicturebackend.model.dto.user.UserRegisterDTO;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jstart.qianyvpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 28435
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-02-21 10:57:53
*/
public interface UserService extends IService<User> {
    /**
     * 测试接口，健康的
     * @return
     */
    String ok();

    /**
     * 数据加密
     * @param text 明文
     * @return 返回密文
     */
    String encryptPassword(String text);

    /**
     * 用户注册
     * @param userRegisterDTO 请求实体
     * @return 主键id
     */
    Long userRegister(UserRegisterDTO userRegisterDTO);

    /**
     * 用户登录
     * @param userLoginDTO 请求实体
     * @param request http请求
     * @return 脱敏后用户数据
     */
    UserVO userLogin(UserLoginDTO userLoginDTO, HttpServletRequest request);

    /**
     * 获取当前用户登录状态，内部调用
     * @param request http请求
     * @return  返回未脱敏用户数据
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     * @param request 请求信息
     * @return  出现成功返回true
     */
    Boolean logout(HttpServletRequest request);

    /**
     * 获取脱敏后信息
     * @param user 原用户信息
     * @return 脱敏后用户信息
     */
    UserVO userToVO(User user);

    /**
     * 用户信息列表批量脱敏
     * @param users 原用户信息
     * @return 脱敏后用户信息
     */
    List<UserVO> UserToVOList(List<User> users);

    /**
     * 获取mybatisPlus查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getUserQueryWrapper(UserQueryRequest userQueryRequest);


    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);


}
