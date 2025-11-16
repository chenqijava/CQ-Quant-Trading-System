package com.nyy.gmail.cloud.framework.dto;

import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class AddTaskDomainDto {

    private String _id;

    private String accid;

    private String groupTaskId;

    private String type;

    private Map checkParams;

    private Date updateTime;

    private Date executeTime;

    private Map params;

    private String userID;

    private String status;

    private Map result;

    private Boolean needFeedback = false;

    private String executeType;

    private String tmpId;

    private Date createTime;

}
