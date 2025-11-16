package com.nyy.gmail.cloud.common.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {

    /**
     * 每秒需要多少个线程处理
     * tasks/(1/taskcost)
     */
    private int corePoolSize = 3;


    /**
     * 线程池维护线程的最大数量
     * (max(tasks)- queueCapacity)/(1/taskcost)
     */
    private int maxPoolSize = 3;

    /**
     * 缓存队列
     * (coreSizePool/taskcost)*responsetime
     */
    private int queueCapacity = 10;

    /**
     * 允许的空闲时间
     * 默认为60
     */
    private int keepAlive = 100;

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //设置核心线程数
        executor.setCorePoolSize(corePoolSize);
        //设置最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        //设置队列容量
        executor.setQueueCapacity(queueCapacity);
        //设置允许的空闲时间（秒）
        //executor.setKeepAliveSeconds(keepAlive);
        //设置默认的线程名称
        executor.setThreadNamePrefix("thread-");
        //设置拒绝策略rejection-policy：当pool已经达到max size的时候，如何处理新任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

//    @Bean
//    public Executor taskVirtualPublishExecutor() {
//        ThreadFactory factory = Thread.ofVirtual().name("task-vthread-publish-", 0).factory();
//        return Executors.newThreadPerTaskExecutor(factory);
//    }
//
//    @Bean
//    public Executor taskVirtualOtherExecutor() {
//        ThreadFactory factory = Thread.ofVirtual().name("task-vthread-other-", 0).factory();
//        return Executors.newThreadPerTaskExecutor(factory);
//    }
//
//    @Bean
//    public Executor taskVirtualRunTaskExecutor() {
//        ThreadFactory factory = Thread.ofVirtual().name("task-vthread-runTask-", 0).factory();
//        return Executors.newThreadPerTaskExecutor(factory);
//    }
//
//    @Bean
//    public Executor taskVirtualCheckTaskExecutor() {
//        ThreadFactory factory = Thread.ofVirtual().name("task-vthread-checkTask-", 0).factory();
//        return Executors.newThreadPerTaskExecutor(factory);
//    }

    @Bean(name = "taskThreadPool")
    public Executor taskThreadPool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50); // 设置核心线程数
        executor.setMaxPoolSize(300); // 设置最大线程数
        executor.setQueueCapacity(100); // 设置队列容量
        executor.setAllowCoreThreadTimeOut(true);  // 允许回收核心线程
        executor.setThreadNamePrefix("taskPool-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}