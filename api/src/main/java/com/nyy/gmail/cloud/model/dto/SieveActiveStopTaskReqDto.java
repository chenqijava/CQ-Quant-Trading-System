package com.nyy.gmail.cloud.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SieveActiveStopTaskReqDto {

    @NotEmpty
    private List<String> ids;
    private Boolean forceStop;
}
