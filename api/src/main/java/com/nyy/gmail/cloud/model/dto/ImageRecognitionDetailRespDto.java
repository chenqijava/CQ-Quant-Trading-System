package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class ImageRecognitionDetailRespDto {

    private String taskId;

    private String id;

    private String status;

    private Map result;

    private Map params;

    private Date createTime;

    private Date finishTime;
}
