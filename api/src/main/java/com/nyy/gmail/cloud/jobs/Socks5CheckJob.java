package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.repository.mongo.Socks5Repository;
import com.nyy.gmail.cloud.service.Socks5Service;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 检测ip是否可用
 * 6分钟跑一次
 */
@Component
@Slf4j
public class Socks5CheckJob {

    @Resource
    private Socks5Service socks5Service;

    @Resource
    private MongoPaginationHelper mongoPaginationHelper;

    @Autowired
    private Socks5Repository socks5Repository;

    private volatile boolean is_running = false;

    @Async("http")
    @Scheduled(cron = "15 0/6 * * * ?")
    public void checkSocks5() {
        if (is_running) {
            return;
        }
        is_running = true;
        long start = System.currentTimeMillis();
        log.info("checkSocks5 start, startTime:{}", System.currentTimeMillis());
        try {
            for (int n = 1; n < Integer.MAX_VALUE; n++) {
                PageResult<Socks5> pageResult = mongoPaginationHelper
                        .query(MongoPaginationBuilder.builder(Socks5.class).enableLog(false).pageSize(100).page(n).build());
                if (pageResult.getData().isEmpty()) {
                    break;
                }
                long begin = System.currentTimeMillis();
                pageResult.getData().forEach(socks5 -> socks5Service.checkNetwork(socks5));
                log.info("check socks5 page {} cost:{}s", n, (System.currentTimeMillis() - begin) / 1000);
            }
            socks5Repository.loadCache();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            is_running = false;
        }
        log.info("checkSocks5 cost:{}s", (System.currentTimeMillis() - start) / 1000);
    }
}
