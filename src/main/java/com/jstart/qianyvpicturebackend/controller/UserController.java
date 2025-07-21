package com.jstart.qianyvpicturebackend.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jstart.qianyvpicturebackend.annotation.AuthCheck;
import com.jstart.qianyvpicturebackend.common.constant.UserConstant;
import com.jstart.qianyvpicturebackend.common.entity.DeleteRequest;
import com.jstart.qianyvpicturebackend.common.entity.Result;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ResultEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import com.jstart.qianyvpicturebackend.model.dto.user.*;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.model.vo.UserVO;
import com.jstart.qianyvpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/user/")
public class UserController {
    @Resource
    private UserService userService;

    @GetMapping("health")
    public Result health() {
        String ok = userService.ok();
        return Result.success(ok);
    }

    /**
     * 用户注册
     *
     * @param userRegisterDTO
     * @return
     */
    @PostMapping("register")
    public Result<Long> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        ThrowUtils.throwIf(userRegisterDTO==null,ResultEnum.PARAMS_ERROR);
        log.info("userRegisterDTO:{}",userRegisterDTO);
        Long userId = userService.userRegister(userRegisterDTO);
        return Result.success(userId);
    }

    /**
     * 用户登录
     *
     * @param userLoginDTO userLoginDTO
     * @param request      请求信息
     * @return 脱敏后用户数据
     */
    @PostMapping("userLogin")
    public Result<UserVO> userLogin(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        //如果请求参数为空，直接抛异常
        ThrowUtils.throwIf(userLoginDTO == null, ResultEnum.PARAMS_ERROR, "参数为空");
        log.info("userDTO:{}", userLoginDTO);
        UserVO userVO = userService.userLogin(userLoginDTO, request);
        return Result.success(userVO);
    }

    /**
     * 获取用户登录态
     *
     * @param request http请求信息
     * @return 脱敏后用户数据
     */
    @GetMapping("getLoginUser")
    public Result<UserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        //返回脱敏后的数据
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(loginUser, userVO);
        return Result.success(userVO);
    }

    /**
     * 用户注销
     *
     * @param request http请求
     * @return 返回成功与否
     */
    @PostMapping("logout")
    public Result<Boolean> loginOut(HttpServletRequest request) {
        return Result.success(userService.logout(request));
    }

    /**
     * 添加用户（管理员）
     *
     * @param userAddRequest
     * @return
     */
    @PostMapping("add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ResultEnum.PARAMS_ERROR, "参数为空");
        log.info("userDTO:{}", userAddRequest);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        //添加默认密码 123157
        user.setUserPassword(userService.encryptPassword("123157"));
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ResultEnum.SYSTEM_ERROR);
        Long userId = user.getId();
        return Result.success(userId);
    }


    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ResultEnum.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ResultEnum.NOT_FOUND_ERROR);
        return Result.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public Result<UserVO> getUserVOById(long id) {
        Result<User> response = getUserById(id);
        User user = response.getData();
        return Result.success(userService.userToVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResultEnum.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return Result.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ResultEnum.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ResultEnum.OPERATION_ERROR);
        return Result.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ResultEnum.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        //进行分页查询
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                                                userService.getUserQueryWrapper(userQueryRequest));
        //先封装一下整体信息
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        //对用户信息脱敏
        List<UserVO> userVOList = userService.UserToVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return Result.success(userVOPage);
    }

}
