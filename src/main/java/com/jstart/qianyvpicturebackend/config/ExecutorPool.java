package com.jstart.qianyvpicturebackend.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ExecutorPool {


    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(
                10, // core pool size
                50, // maximum pool size
                60L, // keep-alive time
                TimeUnit.SECONDS, // time unit for keep-alive time
                new LinkedBlockingQueue<>(40), // work queue
                new MyThreadFactory("label"), // 使用自定义线程工厂
                new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );
    }

}
