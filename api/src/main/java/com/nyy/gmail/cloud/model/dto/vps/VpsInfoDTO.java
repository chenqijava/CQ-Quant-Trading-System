package com.nyy.gmail.cloud.model.dto.vps;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class VpsInfoDTO implements Serializable {

    @JsonFormat(timezone ="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    private Date deadTime;

    private Integer vpsCount;

    private Integer bindCount;

    private Integer unBindCount;

    private String batchId;
}
