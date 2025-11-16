package com.nyy.gmail.cloud.model.bo;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
public class UploadFileBO {
    private String type;
    private String name;
    private String filepath;
}
