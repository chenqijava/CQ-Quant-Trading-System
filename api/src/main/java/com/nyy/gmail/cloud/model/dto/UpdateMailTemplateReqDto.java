package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateMailTemplateReqDto {

    private List<String> ids;

    private Integer status;
}
