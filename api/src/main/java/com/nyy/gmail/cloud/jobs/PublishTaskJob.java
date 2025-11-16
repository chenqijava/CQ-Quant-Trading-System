package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.tasks.BaseTask;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PublishTaskJob {

    @Autowired
    @Qualifier("taskPublishExecutor")
    private Executor taskPublishExecutor;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    private volatile boolean running_task = false;

    @Autowired
    private ApplicationContext applicationContext;


    @Async("publish")
    @Scheduled(cron = "0/2 * * * * ?")
    public void publishTaskJob() {
        if (running_task) {
            log.info("PublishTaskJob is running task!!!");
            return;
        }
        log.info("PublishTaskJob Main Thread start...");
        running_task = true;
        try {
            RLock lock = redissonClient.getLock("PublishTaskJobLock");
            boolean locked = lock.tryLock(3, TimeUnit.SECONDS);
            if (locked) {
                try {
                    List<GroupTask> groupTaskList = groupTaskRepository.findAllWaitPublishTask();
                    while (true) {
                        if (groupTaskList.size() <= 20) {
                            CountDownLatch latch = new CountDownLatch(groupTaskList.size());
                            for (GroupTask groupTask : groupTaskList) {
                                taskPublishExecutor.execute(() -> {
                                    runGroupTask(groupTask, latch);
                                });
                            }
                            log.info("PublishTaskJob Main Thread waiting...1");
                            boolean completed = latch.await(60, TimeUnit.SECONDS);
                            if (completed) {
                                log.info("PublishTaskJob All workers finished.在5分钟之内完成1");
                            } else {
                                log.info("PublishTaskJob finished.部分任务超时未完成1");
                            }
                            break;
                        } else {
                            List<GroupTask> groupTasks = groupTaskList.subList(0, 20);
                            groupTaskList = groupTaskList.subList(20, groupTaskList.size());
                            CountDownLatch latch = new CountDownLatch(groupTasks.size());
                            for (GroupTask groupTask : groupTasks) {
                                taskPublishExecutor.execute(() -> {
                                    runGroupTask(groupTask, latch);
                                });
                            }
                            log.info("PublishTaskJob Main Thread waiting...2");
                            boolean completed = latch.await(60, TimeUnit.SECONDS);
                            if (completed) {
                                log.info("PublishTaskJob All workers finished.在5分钟之内完成2");
                            } else {
                                log.info("PublishTaskJob finished.部分任务超时未完成2");
                            }
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("PublishTaskJob run error: {}", e.getMessage());
        } finally {
            running_task = false;
        }
    }

    private void runGroupTask(GroupTask groupTask, CountDownLatch latch) {
        try {
            log.info("{}  publishTask begin tryLock _id: {}", groupTask.getType(), groupTask.get_id());
            RLock lock2 = redissonClient.getLock("PublishTaskJobLock_" + groupTask.get_id());
            boolean locked2 = lock2.tryLock(3, TimeUnit.SECONDS);
            if (locked2) {
                try {
                    log.info("{}  publishTask begin _id: {}", groupTask.getType(), groupTask.get_id());
                    GroupTask task = groupTaskRepository.findById(groupTask.get_id()).orElse(null);
                    if (task != null) {
                        try {
                            log.info("{}  publishTask found _id: {}", groupTask.getType(), groupTask.get_id());
                            BaseTask baseTask = applicationContext.getBean(task.getType(), BaseTask.class);
                            baseTask.publishTask(task);
                        } catch (Exception e) {
                            log.error("{}  publishTask error _id: {}  error: {}", groupTask.getType(), groupTask.get_id(), e.getMessage());
                        } finally {
                            log.info("{}  publishTask end _id: {}", groupTask.getType(), groupTask.get_id());
                        }
                    } else {
                        log.info("{}  publishTask not found _id: {}", groupTask.getType(), groupTask.get_id());
                    }
                } finally {
                    lock2.unlock();
                }
            }
        } catch (InterruptedException e) {
            log.error("{} PublishTaskJob One Task run error: {}", groupTask.get_id(), e.getMessage());
        } finally {
            latch.countDown();
        }
    }
}
