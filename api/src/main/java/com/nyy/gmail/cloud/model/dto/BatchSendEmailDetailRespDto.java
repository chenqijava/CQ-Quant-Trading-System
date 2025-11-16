package com.nyy.gmail.cloud.model.dto;

import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import lombok.Data;

@Data
public class BatchSendEmailDetailRespDto {
    private Integer successNum=0;
    private Integer openNum=0;
    private Integer replyNum=0;
    private Integer clickNum=0;
    private Integer failNum=0;

    private Integer callbackNum=0; // 退信数

    private GroupTask groupTask;

    private Integer openNumA=0;
    private Integer openNumB=0;

    private Integer clickNumA=0;
    private Integer clickNumB=0;

    private Integer replyNumA=0;
    private Integer replyNumB=0;

    private Integer successNumA=0;
    private Integer successNumB=0;

    private Integer failNumA=0;
    private Integer failNumB=0;

    private Integer totalA=0;
    private Integer totalB=0;
}
