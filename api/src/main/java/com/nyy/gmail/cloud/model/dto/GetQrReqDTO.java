package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GetQrReqDTO implements Serializable {

    /**
     * 分组id
     */
    private String groupID;

}
