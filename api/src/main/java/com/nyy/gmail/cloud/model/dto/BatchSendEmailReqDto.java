package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class BatchSendEmailReqDto {

    private String taskName;

    private String emailAddType = "page"; // page/repo

    private String title;

    private String content;

    private String useAiOptimize="disable"; // enable/disable

    private String addMethod="1"; // 1/2/3

    private String filepath;

    private String addData;

    private String taskId;

    private String action; // reply/noReply/open/noOpen/click/noClick

    private String estimateSendNum; // 预估发送数量

    private String sendMethod="1"; // 1 立即发送 2 定时发送

    private Date sendTime;

    private String loopSend="no"; // yes/no

    private String loopType; // week/day/month

    private String monitorOpen="no"; // yes/no

    private String monitorClick="no"; // yes/no

    private String addUnsubscribe="no";

    private String testAB="no"; // yes/no

    private String titleB;

    private String contentB;

    private Integer percent=20;

    private String testTimeLengthHour="4"; // 测试时长  小时

    private String factor; // reply/open/click/other

    private Integer testNum; // 测试数量

    private String testEmailTaskId;

    private String type; // api/direct/workspace

    private String titleParams;

    private String contentParams;

    private String contactParams;

    private String titleParamsFilepath;

    private String contentParamsFilepath;

    private String contactParamsFilepath;

    private Integer systemEmailCount;

    private String otherEmails;

    private Integer oneAccountSendLimit;

    private Integer failRatePauseTask;

    private Integer availableAccountPauseTask;

    private Integer spamRatePauseTask;

    private String closeFailRatePauseTask;

    private String closeAvailableAccountPauseTask;

    private String closeSpamRatePauseTask;
}
