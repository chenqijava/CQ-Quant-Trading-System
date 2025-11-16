package com.nyy.gmail.cloud.jobs;

import cn.hutool.core.util.StrUtil;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.GroupTaskStatusEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.service.ParamsService;
import com.nyy.gmail.cloud.tasks.sieve.SieveActiveTaskFactory;
import com.nyy.gmail.cloud.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class SieveActiveJob {

    @Autowired
    protected Executor runSieveTaskThreadPool;
    @Autowired
    protected Executor checkSieveTaskThreadPool;

    @Autowired
    private SieveActiveTaskFactory sieveActiveTaskFactory;
    @Autowired
    private GroupTaskRepository groupTaskRepository;
    @Autowired
    private ParamsService paramsService;
    @Autowired
    private RedisUtil redisUtil;

    private final Map<String, AtomicBoolean> runTaskMap = new ConcurrentHashMap<>();

    @Async("asyncSieveTask")
    @Scheduled(initialDelay = 10_000, fixedDelay = 8_000)
    public void runSieveTasks() {
        if (!paramsService.openSieveActiveTask()) {
            return;
        }

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            List<GroupTask> tasks = groupTaskRepository.findAllByTypeAndStatus(TaskTypesEnums.SieveActive.getCode(), GroupTaskStatusEnums.processing.getCode());
            tasks.forEach(task -> {
                AtomicBoolean isTaskRunning = runTaskMap.computeIfAbsent(task.get_id(), k -> new AtomicBoolean(false));
                if (!isTaskRunning.compareAndSet(false, true)) {
                    log.info("任务:【{}】运行任务正在执行，跳过", task.get_id());
                    return;
                }

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        sieveActiveTaskFactory.getTaskBean(task).runTask(task);
                    } finally {
                        isTaskRunning.set(false);
                    }
                }, runSieveTaskThreadPool).whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(StrUtil.format("任务:【{}】执行异常", task.get_id()), ex);
                    }
                });
                futures.add(future);
            });
            log.info("runSieveTasks本次执行任务数: {}", futures.size());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (Exception e) {
            log.error("runSieveTasks执行异常", e);
        }
    }


    private final Map<String, AtomicBoolean> checkTaskMap = new ConcurrentHashMap<>();

    @Async("asyncSieveTask")
    @Scheduled(initialDelay = 10_000, fixedDelay = 8_000)
    public void checkSieveTasks() {
        if (!paramsService.openSieveActiveTask()) {
            return;
        }

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            List<GroupTask> tasks = groupTaskRepository.findAllByTypeAndStatus(TaskTypesEnums.SieveActive.getCode(), GroupTaskStatusEnums.init.getCode());
            tasks.forEach(task -> {
                AtomicBoolean isTaskRunning = checkTaskMap.computeIfAbsent(task.get_id(), k -> new AtomicBoolean(false));
                if (!isTaskRunning.compareAndSet(false, true)) {
                    log.info("任务:【{}】检查任务正在执行，跳过", task.get_id());
                    return;
                }

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        sieveActiveTaskFactory.getTaskBean(task).checkTask(task);
                    } finally {
                        isTaskRunning.set(false);
                    }
                }, checkSieveTaskThreadPool).whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(StrUtil.format("任务:【{}】检查异常", task.get_id()), ex);
                    }
                });
                futures.add(future);
            });
            log.info("checkSieveTasks本次检查任务数: {}", futures.size());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (Exception e) {
            log.error("runSieveTasks检查异常", e);
        }
    }
}
