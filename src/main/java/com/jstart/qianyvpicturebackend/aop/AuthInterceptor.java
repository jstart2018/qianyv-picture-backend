package com.jstart.qianyvpicturebackend.aop;

import com.jstart.qianyvpicturebackend.annotation.AuthCheck;
import com.jstart.qianyvpicturebackend.common.enums.RoleEnum;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ErrorEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@Aspect
@Component
public class AuthInterceptor {
    @Autowired
    private HttpServletRequest request;
    
    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doIntercept(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        //获取当前登录用户,这里获取用户信息的时候就做 用户需要登录的鉴定
        User loginUser = userService.getLoginUser(request);
        //获取注解所带过来的权限，注解带过来的就是表示 所需要的权限
        RoleEnum roleEnum = RoleEnum.getByValue(mustRole);
        //如果在枚举类里（枚举放了所有的权限）没找到对应权限，表示没有特别的权限需求
        if (roleEnum == null){
            joinPoint.proceed();
        }
        //有权限要求：

        //如果 要鉴定的是管理员权限，但登录者没有管理员权限
        if(RoleEnum.ADMIN.equals(roleEnum)&& !Objects.equals(mustRole, loginUser.getUserRole())){
            throw new BusinessException(ErrorEnum.NOT_LOGIN_ERROR,"无权限");
        }



        return joinPoint.proceed();


    }

}
