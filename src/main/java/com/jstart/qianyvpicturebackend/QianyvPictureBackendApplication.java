package com.jstart.qianyvpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan
@EnableAspectJAutoProxy(exposeProxy = true)
public class QianyvPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(QianyvPictureBackendApplication.class, args);
    }

}
