package com.nyy.gmail.cloud.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Data
public class LinkCheckDetail {

    private String link;

    public BigDecimal getRate () {
        if (junkNum + normalNum <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(junkNum).divide(BigDecimal.valueOf(junkNum + normalNum), 6, RoundingMode.HALF_UP);
    }

    private Integer sendNum = 0;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime = new Date();

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date finishTime;

    // 垃圾
    private Integer junkNum = 0;

    // 正常
    private Integer normalNum = 0;

    // 未知
    private Integer unknownNum = 0;

    // 成功
    private Integer success = 0;

    // 失败
    private Integer failed = 0;

    // 其他
    private Integer other = 0;
}
