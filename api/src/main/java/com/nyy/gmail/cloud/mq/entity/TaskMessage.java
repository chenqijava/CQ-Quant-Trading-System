package com.nyy.gmail.cloud.mq.entity;

import com.nyy.gmail.cloud.entity.mongo.SubTask;
import lombok.Data;

import java.util.Map;

@Data
public class TaskMessage {

    private String traceId; // 消息ID

    private SubTask subTask; // 任务

    private Map extendParam; // 扩展参数

    private String accid; // 账号id
}
