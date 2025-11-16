package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.CronTaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.AccountListDTO;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.tasks.CronBaseTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PublishCronTaskJob {

    @Autowired
    private AccountRepository accountRepository;

    private volatile boolean running_task = false;

    private final static CronTaskTypesEnums[] CRON_TASK_TYPES_ENUMS_LIST = {
//            CronTaskTypesEnums.CheckAccountTCPConnection,
//            CronTaskTypesEnums.AccountCheckLogin
    };

    @Autowired
    @Qualifier("taskPublishExecutor")
    private Executor taskPublishExecutor;

    @Autowired
    private ApplicationContext applicationContext;

    @Async("publishCron")
    @Scheduled(cron = "0/2 * * * * ?")
    public void publishCronTaskJob () {
        if (running_task) {
            log.info("PublishCronTaskJob is running task!!!");
            return;
        }
        running_task = true;

        try {
            if (CRON_TASK_TYPES_ENUMS_LIST.length <= 0) {
                return;
            }

            HashMap<String, Object> map = new HashMap<>();
            HashMap<String, Object> map2 = new HashMap<>();
            map2.put("$in", Arrays.asList(AccountOnlineStatus.ONLINE.getCode(), AccountOnlineStatus.WAITING_ONLINE.getCode()));
            map.put("onlineStatus", map2);
            AccountListDTO accountListDTO = new AccountListDTO();
            accountListDTO.setFilters(map);
            int page = 1;
            while (true) {
                PageResult<Account> pageResult = accountRepository.findByPagination(accountListDTO, 20, page);
                if (pageResult.getData().size() <= 0) {
                    break;
                }
                CountDownLatch latch = new CountDownLatch(pageResult.getData().size());
                for (Account account : pageResult.getData()) {
                    taskPublishExecutor.execute(() -> {
                        try {
                            runPublishCronTask(account);
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                log.info("PublishTaskJob Main Thread waiting...1");
                boolean completed = latch.await(60, TimeUnit.SECONDS);
                if (completed) {
                    log.info("PublishTaskJob All workers finished.在5分钟之内完成1");
                } else {
                    log.info("PublishTaskJob finished.部分任务超时未完成1");
                }
                page++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            running_task = false;
        }
    }

    private void runPublishCronTask(Account account) {
        for (CronTaskTypesEnums typesEnums : CRON_TASK_TYPES_ENUMS_LIST) {
            try {
                CronBaseTask baseTask = applicationContext.getBean(typesEnums.getCode(), CronBaseTask.class);
                baseTask.cronAddTask(account);
            } catch (org.springframework.beans.BeansException e) {
                log.error("{}  publishCronTaskJob error _id: {}  error: {}", typesEnums.getCode(), account.get_id(), e.getMessage());
            } finally {
                log.info("{}  publishCronTaskJob end _id: {}", typesEnums.getCode(), account.get_id());
            }
        }
    }

}
