package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.GroupTaskStatusEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SendEmailEventMonitorRepository;
import com.nyy.gmail.cloud.service.BatchSendEmailService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CheckBatchSendEmailTaskReplyJob {

    private volatile boolean is_running = false;

    private volatile boolean is_running_AB = false;

    @Autowired
    private BatchSendEmailService batchSendEmailService;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    @Qualifier("replyMessageExecutor")
    private Executor replyMessageExecutor;
    // 1. 超过30天不检查

    // 2. A/B 优先队列

    @Value("${application.taskType}")
    private String taskType;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SendEmailEventMonitorRepository sendEmailEventMonitorRepository;

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
            // 删除时间监听
            Calendar calendar2 = Calendar.getInstance();
            calendar2.add(Calendar.DATE, -7);
            sendEmailEventMonitorRepository.deleteTimeout(calendar2.getTime());

            log.info("start CheckBatchSendEmailTaskReplyJob");
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, -3);
            // 检查回复邮件
            int page = 1;
            while (true) {
                PageResult<GroupTask> pagination = groupTaskRepository.findByPagination(100, page, Map.of("type", TaskTypesEnums.BatchSendEmail.getCode(), "createTime", Map.of("$gte", calendar.getTime())));
                if (pagination.getData().isEmpty()) {
                    break;
                }
                CountDownLatch latch = new CountDownLatch(pagination.getData().size());
                for (GroupTask groupTask : pagination.getData()) {
                    replyMessageExecutor.execute(() -> {
                        try {
                            RBucket<String> bucket = redissonClient.getBucket("CheckBatchSendEmailTaskReplyJob:" + groupTask.get_id());
                            if (!bucket.isExists()) {
                                batchSendEmailService.checkReply(groupTask);
                                bucket.set("1", Duration.ofMinutes(3));
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                latch.await();
                page++;
            }
        } catch (Exception e) {
            log.info("CheckBatchSendEmailTaskReplyJob error {}", e.getMessage());
        } finally {
            log.info("结束 CheckBatchSendEmailTaskReplyJob");
            is_running = false;
        }
    }

    private volatile boolean is_running2 = false;

    @Async("async")
    @Scheduled(cron = "0 0/5 * * * *")
    public void run2() {
        if (taskType.equals("googleai")) {
            return;
        }
        if (is_running2) {
            return;
        }
        is_running2 = true;
        try {
            log.info("start CheckBatchSendEmailTaskReplyJob2");
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);
            // 检查回复邮件
            int page = 1;
            while (true) {
                PageResult<GroupTask> pagination = groupTaskRepository.findByPagination(5, page, Map.of("type", TaskTypesEnums.BatchSendEmail.getCode(), "createTime", Map.of("$gte", calendar.getTime())));
                if (pagination.getData().isEmpty()) {
                    break;
                }
                CountDownLatch latch = new CountDownLatch(pagination.getData().size());
                for (GroupTask groupTask : pagination.getData()) {
                    batchSendEmailService.checkReply(groupTask);
                }
                latch.await();
                page++;
            }
        } catch (Exception e) {
            log.info("CheckBatchSendEmailTaskReplyJob2 error {}", e.getMessage());
        } finally {
            log.info("结束 CheckBatchSendEmailTaskReplyJob2");
            is_running2 = false;
        }
    }

    private volatile boolean is_running3 = false;

    @Async("async")
    @Scheduled(cron = "0 0 * * * *")
    public void run3() {
        if (taskType.equals("googleai")) {
            return;
        }
        if (is_running3) {
            return;
        }
        is_running3 = true;
        try {
            log.info("start CheckBatchSendEmailTaskReplyJob3");
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -5);
            // 检查回复邮件
            int page = 1;
            while (true) {
                PageResult<GroupTask> pagination = groupTaskRepository.findByPagination(5, page, Map.of("type", TaskTypesEnums.BatchSendEmail.getCode(), "createTime", Map.of("$gte", calendar.getTime())));
                if (pagination.getData().isEmpty()) {
                    break;
                }
                CountDownLatch latch = new CountDownLatch(pagination.getData().size());
                for (GroupTask groupTask : pagination.getData()) {
                    batchSendEmailService.checkReply(groupTask);
                }
                latch.await();
                page++;
            }
        } catch (Exception e) {
            log.info("CheckBatchSendEmailTaskReplyJob3 error {}", e.getMessage());
        } finally {
            log.info("结束 CheckBatchSendEmailTaskReplyJob3");
            is_running3 = false;
        }
    }

    @Async("async")
    @Scheduled(cron = "0/2 * * * * *")
    public void runAB() {
        if (taskType.equals("googleai")) {
            return;
        }
        if (is_running_AB) {
            return;
        }
        is_running_AB = true;
        try {
            // 检查回复邮件
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -30);
            // 检查回复邮件
            int page = 1;
            while (true) {
                // 执行中，开启AB测的
                PageResult<GroupTask> pagination = groupTaskRepository.findByPagination(10, page, Map.of("type", TaskTypesEnums.BatchSendEmail.getCode(), "createTime", Map.of("$gte", calendar.getTime()), "params.reqDto.testAB", "yes", "status", GroupTaskStatusEnums.init.getCode()));
                if (pagination.getData().isEmpty()) {
                    break;
                }
                for (GroupTask groupTask : pagination.getData()) {
                    batchSendEmailService.checkReply(groupTask);
                }
                page++;
            }
        } catch (Exception e) {
            log.info("CheckBatchSendEmailTaskReplyJob AB error {}", e.getMessage());
        } finally {
            log.info("结束 CheckBatchSendEmailTaskReplyJob AB");
            is_running_AB = false;
        }
    }

}
