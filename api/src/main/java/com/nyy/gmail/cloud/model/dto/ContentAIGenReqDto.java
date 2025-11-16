package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ContentAIGenReqDto {
    private String desc;

    private String subject;

    private String content;

    private String aiModel;

    private Integer sendNum = 10;

    private Integer genNum = 10;

    private String source = "0";

    private List<String> emotion = new ArrayList<>();

    private String target;

    private String character;

    private String style;

    private String other;

    private String groupId;

    private String templateId;

    private String contentInfo;
}
