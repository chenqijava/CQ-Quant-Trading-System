package com.nyy.gmail.cloud.framework;

import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.mq.entity.TaskMessage;
import com.nyy.gmail.cloud.service.AccountService;
import com.nyy.gmail.cloud.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class RunTaskRunner implements ApplicationRunner {

    @Autowired
    @Qualifier("taskRunTaskExecutor")
    private Executor taskRunTaskExecutor;

    @Autowired
    private ApplicationContext applicationContext;

    public static final int REQUEST_RONCURRENCY = 2000;

    @Autowired
    private AccountService accountService;

    public static final LinkedTransferQueue<TaskMessage> TASK_QUEUE = new LinkedTransferQueue<TaskMessage>();

//    private static final ReentrantLock lock = new ReentrantLock();
//
//    public static void setRunAccount(String id) {
//        lock.lock();
//        try {
//            RUN_ACCOUNT_IDS.add(id);
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public static void removeRunAccount(String id) {
//        lock.lock();
//        try {
//            RUN_ACCOUNT_IDS.remove(id);
//        } finally {
//            lock.unlock();
//        }
//    }

    private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();
    private static final Set<String> RUN_ACCOUNT_IDS = ConcurrentHashMap.newKeySet();

    private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP2 = new ConcurrentHashMap<>();
    private static final Set<String> RUN_SUBTASK_IDS = ConcurrentHashMap.newKeySet();

    public static void setRunAccount(String id) {
        ReentrantLock lock = LOCK_MAP.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock();
        try {
            RUN_ACCOUNT_IDS.add(id);
        } finally {
            lock.unlock();
            cleanupLock(id, lock);
        }
    }

    public static void removeRunAccount(String id) {
        ReentrantLock lock = LOCK_MAP.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock();
        try {
            RUN_ACCOUNT_IDS.remove(id);
        } finally {
            lock.unlock();
            cleanupLock(id, lock);
        }
    }

    public static void setRunSubTask(String id) {
        ReentrantLock lock = LOCK_MAP2.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock();
        try {
            RUN_SUBTASK_IDS.add(id);
        } finally {
            lock.unlock();
            cleanupLockSubTask(id, lock);
        }
    }

    public static void removeRunSubTask(String id) {
        ReentrantLock lock = LOCK_MAP2.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock();
        try {
            RUN_SUBTASK_IDS.remove(id);
        } finally {
            lock.unlock();
            cleanupLockSubTask(id, lock);
        }
    }

    private static void cleanupLock(String id, ReentrantLock lock) {
        // 只有在锁当前无人使用时才移除
        if (!lock.hasQueuedThreads()) {
            LOCK_MAP.remove(id, lock);
        }
    }

    private static void cleanupLockSubTask(String id, ReentrantLock lock) {
        // 只有在锁当前无人使用时才移除
        if (!lock.hasQueuedThreads()) {
            LOCK_MAP2.remove(id, lock);
        }
    }

    public static boolean containRunAccountId (String id) {
        return RUN_ACCOUNT_IDS.contains(id);
    }

    public static boolean containRunSubTaskId (String id) {
        return RUN_SUBTASK_IDS.contains(id);
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread runner = new Thread(() -> {
            while (true) {
                try {
                    TaskMessage message = TASK_QUEUE.take();
                    taskRunTaskExecutor.execute(() -> {
                        try {
                            setRunAccount(message.getAccid());
                            setRunSubTask(message.getSubTask().get_id());
                            log.info("id: {} type: {} accid: {} start run", message.getSubTask().get_id(), message.getSubTask().getType(), message.getAccid());
                            Task baseTask = applicationContext.getBean(message.getSubTask().getType(), Task.class);
                            Account account = accountService.getOneById(message.getAccid());
                            baseTask.runTask(message.getSubTask(), account);
                        } catch (Exception e) {
                            log.error("id: {} type: {} accid: {} run error: {}", message.getSubTask().get_id(), message.getSubTask().getType(), message.getAccid(), e.getMessage());
                        } finally {
                            removeRunAccount(message.getAccid());
                            removeRunSubTask(message.getSubTask().get_id());
                            log.info("id: {} type: {} accid: {} finish run", message.getSubTask().get_id(), message.getSubTask().getType(), message.getAccid());
                        }
                    });
                } catch (Exception e) {
                    log.error("run task error: {}", e.getMessage());
                }
            }
        });
        runner.setName("RunTaskThread");
        runner.start();
    }
}
