package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class DeadTimeListRespDTO {

    private String _id;

    private String userID;

    private Date deadTime;

    private Long total = 0L;

    private Long bind = 0L;

    private Long unbind = 0L;

    private String deadStatus = "0";

}
