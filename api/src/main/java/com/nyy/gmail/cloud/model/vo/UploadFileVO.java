package com.nyy.gmail.cloud.model.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UploadFileVO {
    private String type;
    private String name;
    private String filepath;
}
