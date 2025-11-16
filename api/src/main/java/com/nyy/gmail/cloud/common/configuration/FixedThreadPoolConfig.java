package com.nyy.gmail.cloud.common.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 线程池配置,用来异步保存数据到数据库
 */
@Configuration
@EnableAsync
public class FixedThreadPoolConfig {

    @Bean(name = "fixedThreadPool")
    public Executor fixedThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 设置核心线程数
        executor.setMaxPoolSize(10); // 设置最大线程数
        executor.setQueueCapacity(10000); // 设置队列容量
        executor.setThreadNamePrefix("FixedThreadPool-");
        executor.initialize();
        return executor;
    }
}