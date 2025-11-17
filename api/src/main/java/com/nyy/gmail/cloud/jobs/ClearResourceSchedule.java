package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.model.dto.AccountListDTO;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.utils.CaptchaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ClearResourceSchedule {


    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private AccountRepository accountRepository;


    @Async("async")
    @Scheduled(cron = "0 0 4 * * ?") // 每天四点执行
    public void clearExpiredResource() {
        log.info("开始清理图形验证码");
        CaptchaUtils.clearCaptchaImage();
        log.info("清理图形验证码完成");
    }
}
