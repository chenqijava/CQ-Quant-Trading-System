package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class ImageRecognitionDetailReqDto {
    private String taskId;
    private String status;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
