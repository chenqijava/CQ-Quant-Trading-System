package com.nyy.gmail.cloud.model.dto.task;

import lombok.Data;

@Data
public class MsgTaskQueryDTO {

    /**
     * 任务描述
     */
    private String taskDesc;

    private String status;
}
