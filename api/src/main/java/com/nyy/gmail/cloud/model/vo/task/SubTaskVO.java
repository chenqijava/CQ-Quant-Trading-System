package com.nyy.gmail.cloud.model.vo.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class SubTaskVO {

    private String sendId;

    private String sendPhone;

    private String taskId;

    private String receiveId;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date finishTime;

    private String content;

    private Map<String, Object> params;

    private Map<String, Object> checkParams;

    private Map result;
}
