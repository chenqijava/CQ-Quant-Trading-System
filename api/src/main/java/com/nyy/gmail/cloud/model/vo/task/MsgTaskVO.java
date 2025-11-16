package com.nyy.gmail.cloud.model.vo.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class MsgTaskVO {

    private String _id;

    private String userID;

    private String mark;

    private String desc;

    private List<String> ids;

    private String type;

    private Map params;

    private String platformTaskId;

    private String status; // waitPublish -> processing -> init -> pause/success/failed

    private String executeType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date executeTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date finishTime;

    private Long total = 0L;

    private Long success = 0L;

    private Long failed = 0L;




}
