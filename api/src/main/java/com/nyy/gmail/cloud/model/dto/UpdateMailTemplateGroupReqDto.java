package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateMailTemplateGroupReqDto {

    private List<String> ids;

    private String groupId;
}
