package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.GoogleStudioApiKey;
import com.nyy.gmail.cloud.enums.AiTypeEnums;
import com.nyy.gmail.cloud.model.dto.AccountPlatformReqDto;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.repository.mongo.GoogleStudioApiKeyRepository;
import com.nyy.gmail.cloud.service.GoogleStudioApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class FirstCheckGoogleApiJob {

    private volatile boolean is_running = false;

    @Autowired
    private GoogleStudioApiKeyRepository googleStudioApiKeyRepository;

    @Autowired
    private GoogleStudioApiKeyService googleStudioApiKeyService;

    @Autowired
    @Qualifier("taskOtherExecutor")
    private Executor taskExecutor;

    private volatile int chatgptDisablePage = 0;

    @Async("async")
    @Scheduled(cron = "0/2 * * * * *")
    public void run() {
        if (is_running) {
            return;
        }
        is_running = true;
        try {
            log.info("开始FirstCheckGoogleApi");
            List<GoogleStudioApiKey> list = googleStudioApiKeyRepository.findByNeedCheck();
//            if (list == null || list.isEmpty()) {
//                AccountPlatformReqDto accountPlatformReqDto = new AccountPlatformReqDto();
//                accountPlatformReqDto.setSorter(Map.of("_id", 1));
//                accountPlatformReqDto.setFilters(Map.of("type", AiTypeEnums.Chatgpt.getCode(), "status", "disable"));
//                PageResult<GoogleStudioApiKey> byPagination = googleStudioApiKeyRepository.findByPagination(accountPlatformReqDto, 10, chatgptDisablePage);
//                if (byPagination != null && !byPagination.getData().isEmpty()) {
//                    googleStudioApiKeyRepository.updateBatchCheck(byPagination.getData().stream().map(GoogleStudioApiKey::get_id).toList(), "0");
//                    chatgptDisablePage = chatgptDisablePage + 1;
//                } else {
//                    chatgptDisablePage = 0;
//                }
//            }
//            list = googleStudioApiKeyRepository.findByNeedCheck();

            Collections.shuffle(list);
            list = list.subList(0, Math.min(list.size(), 10));
            CountDownLatch latch = new CountDownLatch(list.size());
            for (GoogleStudioApiKey apiKey : list) {
                taskExecutor.execute(() -> {
                    try {
                        IdsListDTO idsListDTO = new IdsListDTO();
                        idsListDTO.setId(apiKey.get_id());
                        googleStudioApiKeyService.test(idsListDTO, apiKey.getUserID());
                        googleStudioApiKeyRepository.updateIsCheck(apiKey.get_id());
                    } catch (Exception e) {
                        log.info("检测API错误:" + apiKey.get_id() + " info:" + e.getMessage());
                        if (new Date().getTime() - apiKey.getCreateTime().getTime() > 3600 * 1000) {
                            googleStudioApiKeyRepository.updateDisable(apiKey.get_id());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        } catch (InterruptedException e) {
        } finally {
            log.info("结束FirstCheckGoogleApi");
            is_running = false;
        }
    }

}
