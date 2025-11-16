package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchSendEmailTestReqDto extends BatchSendEmailReqDto {
    private Integer count;
    private String groupTaskId;

    private List<String> ids;
}
