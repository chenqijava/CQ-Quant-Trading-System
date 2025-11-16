package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImageRecognitionReqDto {
    private List<String> linkList;
    private String promptVersion;
    private String projectName;
}
