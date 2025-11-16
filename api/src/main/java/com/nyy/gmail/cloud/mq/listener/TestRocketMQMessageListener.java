package com.nyy.gmail.cloud.mq.listener;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.Message;
import com.nyy.gmail.cloud.enums.TagsEnums;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RocketMQMessageListener(topic = "GMAIL_TOPIC", consumerGroup = "gmail-cloud-consumer-group-default", selectorExpression = TagsEnums.DEFAULT_TAGS)
public class TestRocketMQMessageListener implements RocketMQListener<Message> {


    @Override
    public void onMessage(Message message) {
        log.info("onMessage: {}", JSON.toJSONString(message));

        String tag = message.getTag();

        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("tag: {}", tag);
        log.info("body: {}", body);

    }

}