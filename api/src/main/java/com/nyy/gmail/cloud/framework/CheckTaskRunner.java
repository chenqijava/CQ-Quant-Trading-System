package com.nyy.gmail.cloud.framework;

import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.GroupTaskStatusEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.framework.mq.SubTaskMQMessageListener;
import com.nyy.gmail.cloud.mq.entity.TaskMessage;
import com.nyy.gmail.cloud.service.AccountService;
import com.nyy.gmail.cloud.service.GroupTaskService;
import com.nyy.gmail.cloud.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class CheckTaskRunner implements ApplicationRunner {

    @Autowired
    @Qualifier("taskCheckTaskExecutor")
    private Executor taskCheckTaskExecutor;

    @Autowired
    private ApplicationContext applicationContext;

    private static final int maxConcurrent = 2000;

    @Autowired
    private GroupTaskService groupTaskService;

    @Autowired
    private AccountService accountService;

    private final Map<String, Date> LastCronCheckTaskTime = new HashMap<>();

    @Value("${application.taskType}")
    private String taskType;

    @Autowired
    private TaskSchedulingService taskSchedulingService;

    @Override
    public void run(ApplicationArguments args) {
        Thread runner = new Thread(() -> {
            // 初始化从数据库加载任务
            if (taskType.equals("publish")) {
                taskSchedulingService.loopFindSubTask();
            }
            while (true) {
                try {
                    Thread.sleep(2000);
                    int size = SubTaskMQMessageListener.ACCOUNT_MAP.size();
                    int limit = Math.min(size, maxConcurrent);
                    CountDownLatch latch = new CountDownLatch(limit);
                    ArrayList<String> accids = new ArrayList<>(SubTaskMQMessageListener.ACCOUNT_MAP.keySet());
                    log.info("checkTask 等待运行的账号数量 {}", accids.size());
                    List<String> emptyIds = new ArrayList<>();
                    for (int i = 0; i < limit; i++) {
                        String accid = accids.get(i);
                        taskCheckTaskExecutor.execute(() -> {
                            try {
                                log.info("checkTask 开始 {}", accid);
                                LastCronCheckTaskTime.put(accid, new Date());
                                Account account = null;

                                if (RunTaskRunner.containRunAccountId(accid)) {
                                    log.info("checkTask accid: {} 正在运行", accid);
                                    return;
                                }
                                if (RunTaskRunner.TASK_QUEUE.size() >= RunTaskRunner.REQUEST_RONCURRENCY) {
                                    // 执行队列已满
                                    log.info("checkTask accid: {} 执行队列已满", accid);
                                    return;
                                }
                                List<TaskMessage> taskMessages = SubTaskMQMessageListener.ACCOUNT_MAP.get(accid);
                                TaskMessage nextRunMessage = null;
                                Date now = new Date();
                                List<String> checkTaskTrueTypes = new ArrayList<>();
                                for (int j = taskMessages.size() - 1; j >= 0; j--) {
                                    TaskMessage message = taskMessages.get(j);
                                    try {
                                        if (nextRunMessage != null && TaskTypesEnums.fromCode(message.getSubTask().getType()).getPriority() <= TaskTypesEnums.fromCode(nextRunMessage.getSubTask().getType()).getPriority()) {
                                            continue;
                                        }
                                        if (StringUtils.isEmpty(message.getSubTask().getGroupTaskId())) {
                                            nextRunMessage = message;
                                            break;
                                        }
                                        GroupTask groupTask = groupTaskService.findById(message.getSubTask().getGroupTaskId());
                                        if (groupTask == null || groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode()) || groupTask.getStatus().equals(GroupTaskStatusEnums.failed.getCode())) {
                                            nextRunMessage = message;
                                            break;
                                        }
                                        if (groupTask.getStatus().equals(GroupTaskStatusEnums.pause.getCode())) {
                                            continue;
                                        }
                                        if (message.getSubTask().getExecuteTime() != null && message.getSubTask().getExecuteTime().compareTo(now) <= 0) {
                                            Task baseTask = applicationContext.getBean(message.getSubTask().getType(), Task.class);
                                            if (account == null) {
                                                account = accountService.getOneById(message.getAccid());
                                            }
                                            if (account == null) {
                                                TaskTypesEnums typesEnums = TaskTypesEnums.fromCode(message.getSubTask().getType());
                                                checkTaskTrueTypes.add(typesEnums.getCode() + "_" + typesEnums.getPriority());
                                                nextRunMessage = message;
                                            } else {
                                                if (baseTask.checkTask(message.getSubTask(), account, now)) {
                                                    TaskTypesEnums typesEnums = TaskTypesEnums.fromCode(message.getSubTask().getType());
                                                    checkTaskTrueTypes.add(typesEnums.getCode() + "_" + typesEnums.getPriority());
                                                    nextRunMessage = message;
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.info("account:{} check task:{} error:{}", message.getAccid(), message.getSubTask().get_id(), e.getMessage());
                                    }
                                }
                                // 重复放入
                                if (nextRunMessage != null) {
                                    if (RunTaskRunner.containRunSubTaskId(nextRunMessage.getSubTask().get_id())) {
                                        // 已经再运行就直接删除
                                        taskMessages.remove(nextRunMessage);
                                        if (taskMessages.isEmpty()) {
                                            emptyIds.add(nextRunMessage.getAccid());
                                        }
                                        log.info("checkTask isRunning", accid);
                                    } else {
                                        if (RunTaskRunner.TASK_QUEUE.hasWaitingConsumer()) {
                                            try {
                                                RunTaskRunner.TASK_QUEUE.transfer(nextRunMessage); // 立即交付
                                            } catch (InterruptedException e) {
                                            }
                                        } else {
                                            RunTaskRunner.TASK_QUEUE.put(nextRunMessage); // 正常入队
                                        }
                                        taskMessages.remove(nextRunMessage);
                                        if (taskMessages.isEmpty()) {
                                            emptyIds.add(nextRunMessage.getAccid());
                                        }
                                        Date date = LastCronCheckTaskTime.get(accid);
                                        log.info("add push to queue account:{} task:{} type:{} checkTime: {} checkTaskTrueTypes:{}", nextRunMessage.getAccid(), nextRunMessage.getSubTask().get_id(),
                                                nextRunMessage.getSubTask().getType(), new Date().getTime() - date.getTime(), String.join(",", checkTaskTrueTypes));
                                    }
                                } else {
                                    log.info("checkTask nextRunMessage is null", accid);
                                }
                            } finally {
                                latch.countDown();
                            }
                        });
                    }
                    latch.await();
                    SubTaskMQMessageListener.clearAccountMap(emptyIds);
                } catch (Exception e) {
                    log.error("run task error: {}", e.getMessage());
                }
            }
        });
        runner.setName("CheckTaskThread");
        runner.start();
    }
}
