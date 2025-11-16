package com.nyy.gmail.cloud.common.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ScheduleExecutorConfig {

    @Bean
    public Executor async() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("schedule-asy-");
        executor.setMaxPoolSize(50);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(600);
        executor.setQueueCapacity(1000);//添加队列容量
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    @Bean
    public Executor asyncSieveTask() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("sieve-asy-");
        executor.setMaxPoolSize(20);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(600);
        executor.setQueueCapacity(100);//添加队列容量
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    @Bean
    public Executor runSieveTaskThreadPool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize * 2); // 设置核心线程数
        executor.setMaxPoolSize(corePoolSize * 4); // 设置最大线程数
        executor.setQueueCapacity(100); // 设置队列容量
        executor.setAllowCoreThreadTimeOut(true);  // 允许回收核心线程
        executor.setThreadNamePrefix("runSieveTaskPool-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    @Bean
    public Executor checkSieveTaskThreadPool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize * 2); // 设置核心线程数
        executor.setMaxPoolSize(corePoolSize * 4); // 设置最大线程数
        executor.setQueueCapacity(100); // 设置队列容量
        executor.setAllowCoreThreadTimeOut(true);  // 允许回收核心线程
        executor.setThreadNamePrefix("checkSieveTaskPool-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

}
