package com.nyy.gmail.cloud.framework.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.aliyun.openservices.ons.api.Message;
import com.nyy.gmail.cloud.enums.TagsEnums;
import com.nyy.gmail.cloud.mq.entity.TaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RocketMQMessageListener(topic = "GMAIL_TOPIC", consumerGroup = "gmail-cloud-consumer-group", selectorExpression = TagsEnums.SUBTASK_RUN_TAGS)
public class SubTaskMQMessageListener implements RocketMQListener<Message> {

    public final static ConcurrentHashMap<String, List<TaskMessage>> ACCOUNT_MAP = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();

    private static ReentrantLock getLock(String key) {
        return LOCK_MAP.computeIfAbsent(key, k -> new ReentrantLock());
    }

    private static void cleanupLock(String key, ReentrantLock lock) {
        if (!lock.hasQueuedThreads() && !lock.isLocked()) {
            LOCK_MAP.remove(key, lock);
        }
    }

    public static void clearAccountMap () {
        for (Map.Entry<String, List<TaskMessage>> entry : ACCOUNT_MAP.entrySet()) {
            String id = entry.getKey();
            ReentrantLock lock = getLock(id);
            lock.lock();
            try {
                List<TaskMessage> list = entry.getValue();
                if (list == null || list.isEmpty()) {
                    ACCOUNT_MAP.remove(id);
                }
            } finally {
                lock.unlock();
                cleanupLock(id, lock);
            }
        }
    }

    public static void addAccountMap (TaskMessage taskMessage) {
        String accid = taskMessage.getAccid();
        ReentrantLock lock = getLock(accid);
        lock.lock();
        try {
            ACCOUNT_MAP.computeIfAbsent(accid, k -> new ArrayList<>()).add(taskMessage);
        } finally {
            lock.unlock();
            cleanupLock(accid, lock);
        }
    }

    public static void clearAccountMap(List<String> emptyIds) {
        for (String id : emptyIds) {
            ReentrantLock lock = getLock(id);
            lock.lock();
            try {
                List<TaskMessage> list = ACCOUNT_MAP.get(id);
                if (list == null || list.isEmpty()) {
                    ACCOUNT_MAP.remove(id);
                }
            } finally {
                lock.unlock();
                cleanupLock(id, lock);
            }
        }
    }

    @Override
    public void onMessage(Message message) {
        log.info("SUBTASK_RUN_TAGS onMessage: {}", JSON.toJSONString(message));

        String tag = message.getTag();

        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("SUBTASK_RUN_TAGS start tag: {} body: {}", tag, body);

        try {
            if (body.startsWith("{")) {
                TaskMessage taskMessage = JSON.parseObject(body, TaskMessage.class);
                addAccountMap(taskMessage);
            } else {
                JSONArray jsonArray = JSON.parseArray(body);
                for (int i = 0; i < jsonArray.size(); i++) {
                    TaskMessage taskMessage = jsonArray.getObject(i, TaskMessage.class);
                    addAccountMap(taskMessage);
                }
            }
        } catch (Exception e) {
            log.error("SUBTASK_RUN_TAGS consume failed tag: {}, body: {}", tag, body);
        }
    }
}