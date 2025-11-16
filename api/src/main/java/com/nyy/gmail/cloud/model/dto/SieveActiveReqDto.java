package com.nyy.gmail.cloud.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SieveActiveReqDto {

    @NotEmpty
    private String desc;

    @NotEmpty
    private String project;

    @NotEmpty
    private String filepath;

}
