package com.nyy.gmail.cloud.model.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MsgTaskDTO {

    private String taskDesc;

    private String text;

    private String userId;

    private List<String> ids;

    private List<String> accountGroupIds;

    private String  addMethod;

    private String addData;

    private String addDataType;

    private String addDataFilePath;


    private String fileName;

    private String filePath;

    private String activeTabKey;

    private String sendMethod;

    //@JsonFormat(timezone ="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    private Date sendDateTime;

    private String images;

}
