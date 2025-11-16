package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class ImportAccountReqDto {

    private String filepath;

    private String type; // mobile/web/outlook graph

    private String groupId;

    private String openExportReceiveCode = "0";
}
