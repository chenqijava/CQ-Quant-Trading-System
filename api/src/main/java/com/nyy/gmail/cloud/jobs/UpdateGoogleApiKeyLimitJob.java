package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.repository.mongo.GoogleStudioApiKeyRepository;
import com.nyy.gmail.cloud.service.AiStatisticsService;
import com.nyy.gmail.cloud.service.GoogleStudioApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;

@Slf4j
@Component
public class UpdateGoogleApiKeyLimitJob {

    private volatile boolean is_running = false;

    @Autowired
    private GoogleStudioApiKeyRepository googleStudioApiKeyRepository;

    @Autowired
    private GoogleStudioApiKeyService googleStudioApiKeyService;

    @Autowired
    private AiStatisticsService aiStatisticsService;

    @Async("async")
    @Scheduled(cron = "0 * * * * *")
    public void run() {
        if (is_running) {
            return;
        }
        is_running = true;
        try {
            log.info("开始更新Google API LIMIT");
            int i = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int m = Calendar.getInstance().get(Calendar.MINUTE);
            if (i == 15 && m == 0) {
                // 统计当天的
                aiStatisticsService.statistics();
                log.info("开始更新 statistics");
                googleStudioApiKeyRepository.updateUseLimit(0, 0);
                log.info("开始更新 updateUseLimit");
                aiStatisticsService.newStatistics();
                log.info("开始更新 newStatistics");
            } else {
                aiStatisticsService.statistics();
                googleStudioApiKeyRepository.updateUseLimit(null, 0);
            }
        } finally {
            log.info("结束更新Google API LIMIT");
            is_running = false;
        }
    }
}
