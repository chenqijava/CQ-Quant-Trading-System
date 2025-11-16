package com.nyy.gmail.cloud.mq.sender;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@Slf4j
public class RocketMqProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;


    @Value("${rocketmq.rocketMqSendMaxRetry:3}")
    private Integer rocketMqSendMaxRetry;


    /**
     * 推送消息
     *
     * @param topic
     * @param tag
     * @param content
     */
    public void sendMessage(String topic, String tag, String content, int count, String key) {
        log.info("sendMessage topic:{}, tag:{}, content:{}", topic, tag, content);
        // 超过重试次数时记录错误
        if (count >= rocketMqSendMaxRetry) {
            log.error("push message to mq error,beyond retry count,count:{}", rocketMqSendMaxRetry);
            return;
        }
        try {
            // 通用消息包装
            Message message = new Message(topic, tag, Optional.ofNullable(key).orElse(""), content.getBytes(StandardCharsets.UTF_8));
            SendResult sendResult = rocketMQTemplate.syncSend(topic + ":" + tag, message);
            log.info("sendResult:{}", JSON.toJSONString(sendResult));
            System.out.println("sendResult" + JSON.toJSONString(sendResult));
        } catch (Exception e) {
            // 发送失败时重试
            log.error("push message to mq error,retry it.error:{}", e);
            sendMessage(topic, tag, content, count + 1, key);
        }
    }
}
