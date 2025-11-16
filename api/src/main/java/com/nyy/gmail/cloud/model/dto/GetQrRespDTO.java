package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class GetQrRespDTO {

    // 二维码
    private String qr;

    // 过期时间, 秒
    private int expire;

    // accid
    private String accid;

    /**
     * 群组ID
     */
    private String groupID;
}
