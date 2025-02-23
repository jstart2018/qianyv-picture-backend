package com.jstart.qianyvpicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Preconditions;
import com.jstart.qianyvpicturebackend.annotation.AuthCheck;
import com.jstart.qianyvpicturebackend.common.constant.UserConstant;
import com.jstart.qianyvpicturebackend.common.enums.RoleEnum;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ErrorEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import com.jstart.qianyvpicturebackend.model.dto.UserDTO;
import com.jstart.qianyvpicturebackend.model.dto.user.UserLoginDTO;
import com.jstart.qianyvpicturebackend.model.dto.user.UserQueryRequest;
import com.jstart.qianyvpicturebackend.model.dto.user.UserRegisterDTO;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.model.vo.UserVO;
import com.jstart.qianyvpicturebackend.service.UserService;
import com.jstart.qianyvpicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author 28435
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-02-21 10:57:53
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    @Override
    public String ok() {
        return "ok";
    }

    @Override
    public String encryptPassword(String text) {
        String salt = "jstart";

        //return DigestUtils.md5DigestAsHex(text.getBytes());
        //加盐：
        return DigestUtils.md5DigestAsHex((salt + text).getBytes());
    }


    @Override
    public Long userRegister(UserRegisterDTO userRegisterDTO) {
        //参数校验
        Preconditions.checkArgument(userRegisterDTO != null, "userDTO is null");
        Preconditions.checkArgument(!StringUtils.isBlank(userRegisterDTO.getUserAccount()), "账号不能为空");
        Preconditions.checkArgument(!StringUtils.isBlank(userRegisterDTO.getUserPassword()), "密码不能为空");
        Preconditions.checkArgument(!StringUtils.isBlank(userRegisterDTO.getCheckPassword()), "确认密码不能为空");
        ThrowUtils.throwIf(userRegisterDTO.getUserAccount().length()<6,
                            ErrorEnum.PARAMS_ERROR,"账号长度需大于6位");
        ThrowUtils.throwIf(userRegisterDTO.getUserPassword().length()<6,
                ErrorEnum.PARAMS_ERROR,"密码长度需大于6位");
        if (!userRegisterDTO.getUserPassword().equals(userRegisterDTO.getCheckPassword())){
            throw new BusinessException(ErrorEnum.PARAMS_ERROR,"两次密码不一致");
        }
        //判断数据库有无重复账号
        QueryWrapper<User> queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userRegisterDTO.getUserAccount());
        Long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorEnum.PARAMS_ERROR, "账号已存在");
        }
        //密码加密
        String encryptPassword = encryptPassword(userRegisterDTO.getUserPassword());
        //填充默认信息
        User user = new User();
        BeanUtils.copyProperties(userRegisterDTO,user);
        user.setUserPassword(encryptPassword);
        user.setUserName("语友"+ (10000+new Random().nextInt(90000)));
        user.setUserRole(RoleEnum.USER.getValue());
        //插入数据库
        if (log.isInfoEnabled())
            log.info("user:{}", user);
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save,ErrorEnum.SYSTEM_ERROR,"注册失败，数据库异常");
        return user.getId();
    }

    @Override
    public UserVO userLogin(UserLoginDTO userLoginDTO, HttpServletRequest request) {
        //1、校验参数
        Preconditions.checkArgument(!StringUtils.isBlank(userLoginDTO.getUserAccount()), "账号不能为空");
        Preconditions.checkArgument(!StringUtils.isBlank(userLoginDTO.getUserPassword()), "密码不能为空");

        //2、账号密码验证
        //3、获取用户数据
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userLoginDTO.getUserAccount());
        queryWrapper.eq("userPassword",encryptPassword(userLoginDTO.getUserPassword()));//加密
        User user = this.baseMapper.selectOne(queryWrapper);

        ThrowUtils.throwIf(user==null,ErrorEnum.OPERATION_ERROR,"密码错误或用户不存在");

        //4、记录登录状态
        request.getSession().setAttribute(UserConstant.user_login_status,user);

        //5、返回脱敏后用户数据
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);

        return userVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        //1、获取用户session的值
        User user = (User)request.getSession().getAttribute(UserConstant.user_login_status);
        ThrowUtils.throwIf(user == null,ErrorEnum.NOT_LOGIN_ERROR);
        //2、根据返回的用户值，查询数据库（避免数据已经被更新，而session缓存中没有更新）
        User u = this.baseMapper.selectById(user.getId());
        ThrowUtils.throwIf(u == null,ErrorEnum.NOT_LOGIN_ERROR);

        //3、返回用户数据
        return u;
    }

    @Override
    public Boolean logout(HttpServletRequest request) {
        //判断用户是否已经登录，已经登录才能注销
        Object attribute = request.getSession().getAttribute(UserConstant.user_login_status);
        ThrowUtils.throwIf(attribute == null,ErrorEnum.NOT_LOGIN_ERROR);
        //获取session，删除对应的key
        try {
            request.getSession().removeAttribute(UserConstant.user_login_status);
        } catch (Exception e) {
            throw new BusinessException(ErrorEnum.OPERATION_ERROR,"注销失败");
        }
        return true;
    }

    @Override
    public UserVO userToVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        return userVO;
    }

    @Override
    public List<UserVO> UserToVOList(List<User> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserVO> userVOList = new ArrayList<>();
        for (User user : users) {
            UserVO userVO = this.userToVO(user);
            BeanUtils.copyProperties(user,userVO);
            userVOList.add(userVO);
        }
        return userVOList;
    }

    @Override
    public QueryWrapper<User> getUserQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorEnum.PARAMS_ERROR, "请求参数为空");
        }
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }



}




