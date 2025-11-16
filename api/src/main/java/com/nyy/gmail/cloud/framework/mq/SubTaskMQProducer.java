package com.nyy.gmail.cloud.framework.mq;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.Message;
import com.nyy.gmail.cloud.enums.TagsEnums;
import com.nyy.gmail.cloud.mq.entity.TaskMessage;
import com.nyy.gmail.cloud.utils.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class SubTaskMQProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private final static String topic = "GMAIL_TOPIC";

    private final static String tag = TagsEnums.SUBTASK_RUN_TAG;

    @Value("${rocketmq.rocketMqSendMaxRetry:3}")
    private Integer rocketMqSendMaxRetry;

    /**
     * 推送消息
     *
     * @param content
     */
    public void sendMessage(String content) {
        sendMessage(content, 0);
    }

    public void sendMessage(String content, int count) {
        log.info("sendMessage topic:{}, tag:{}, content:{}", topic, tag, content);
        // 超过重试次数时记录错误
        if (count >= rocketMqSendMaxRetry) {
            log.error("push message to mq error,beyond retry count,count:{}", rocketMqSendMaxRetry);
            return;
        }
        try {
            // 通用消息包装
            Message message = new Message(topic, tag, "", content.getBytes(StandardCharsets.UTF_8));
            SendResult sendResult = rocketMQTemplate.syncSend(topic + ":" + tag, message);
            log.info("sendResult:{}", JSON.toJSONString(sendResult));
        } catch (Exception e) {
            // 发送失败时重试
            log.error("push message to mq error,retry it.error:{}", e);
            sendMessage(content, count + 1);
        }
    }

    public void sendMessage(TaskMessage message) {
        if (StringUtils.isEmpty(message.getTraceId())) {
            message.setTraceId(UUIDUtils.get32UUId());
        }
        sendMessage(JSON.toJSONString(message), 0);
    }

    public void sendMessage(List<TaskMessage> messages) {
        messages.forEach(e -> {
            if (StringUtils.isEmpty(e.getTraceId())) {
                e.setTraceId(UUIDUtils.get32UUId());
            }
        });
        sendMessage(JSON.toJSONString(messages), 0);
    }
}
