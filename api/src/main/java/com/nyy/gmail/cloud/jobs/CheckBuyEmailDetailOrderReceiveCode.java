package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.service.BuyEmailOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Slf4j
@Component
public class CheckBuyEmailDetailOrderReceiveCode {

    private volatile boolean is_running = false;

    @Autowired
    private BuyEmailOrderService  buyEmailOrderService;

    @Value("${application.taskType}")
    private String taskType;

    @Async("async")
    @Scheduled(cron = "0/2 * * * * *")
    public void run() {
        if (taskType.equals("googleai")) {
            return;
        }
        if (is_running) {
            return;
        }
        is_running = true;
        try {
            // 检查接码
            buyEmailOrderService.receiveEmail();
        } catch (Exception e) {
            log.info("CheckBuyEmailDetailOrderReceiveCode error {}", e.getMessage());
        } finally {
            log.info("结束 CheckBuyEmailDetailOrderReceiveCode");
            is_running = false;
        }
    }

}
