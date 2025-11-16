package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class ImportMailTemplateReqDto {

    private String filepath;

    private String type; // 0 页面录入 1 文件上传

    private String groupId;

    private String name;

    private String title;

    private String content;

    private Integer templateType = 0;

    private String UserId;
}
