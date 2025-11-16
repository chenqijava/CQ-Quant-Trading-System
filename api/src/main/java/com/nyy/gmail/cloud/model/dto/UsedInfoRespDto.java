package com.nyy.gmail.cloud.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class UsedInfoRespDto {

    private String platformId;

    private String platformName;

    private Boolean used;

    private Boolean realUsed;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date usedTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date realUsedTime;
}
