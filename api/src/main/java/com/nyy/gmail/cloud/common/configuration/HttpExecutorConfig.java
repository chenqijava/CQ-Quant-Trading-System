package com.nyy.gmail.cloud.common.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
public class HttpExecutorConfig {

    @Bean
    public Executor http() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("http-asy-");
        executor.setMaxPoolSize(50);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor other() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("other-asy-");
        executor.setMaxPoolSize(50);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor publishCron() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("publishCron-asy-");
        executor.setMaxPoolSize(50);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor publish() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("publish-asy-");
        executor.setMaxPoolSize(50);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor pullEmail() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("pullEmail-asy-");
        executor.setMaxPoolSize(50);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
    

    @Bean
    public Executor translate() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("translate-asy-");
        executor.setMaxPoolSize(50);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
    @Bean
    public Executor dealInvalidSocks5() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("dealInvalidSocks5-asy-");
        executor.setMaxPoolSize(50);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor taskPublishExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("taskPublish-asy-");
        executor.setMaxPoolSize(100);
        executor.setCorePoolSize(20);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor taskOtherExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("taskOther-asy-");
        executor.setMaxPoolSize(1000);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor taskCheckTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("taskCheckTask-asy-");
        executor.setMaxPoolSize(500);
        executor.setCorePoolSize(150);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor taskRunTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("taskRunTask-asy-");
        executor.setMaxPoolSize(1500);
        executor.setCorePoolSize(200);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor receiveCodeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("receiveCode-asy-");
        executor.setMaxPoolSize(1500);
        executor.setCorePoolSize(200);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(100);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ai-asy-");
        executor.setMaxPoolSize(100);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(100);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public Executor replyMessageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("replyMessage-asy-");
        executor.setMaxPoolSize(100);
        executor.setCorePoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(100);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor CheckEmailActiveExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("CheckEmailActive-asy-");
        executor.setMaxPoolSize(1500);
        executor.setCorePoolSize(200);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
