package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class DeleteMailTemplateGroupReqDto {

    private List<String> ids;
}
