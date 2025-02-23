package com.jstart.qianyvpicturebackend.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) //作用范围：方法上
@Retention(RetentionPolicy.RUNTIME) //注解保留到运行时
public @interface AuthCheck {

    //定义属性：

    /**
     * 必须的权限，默认值为空字符串
     * 可以作为一个约定：打上了这个注解就要判断是否登录，如果不需要登录就不打
     * @return 权限
     */
    String mustRole() default "";

}
