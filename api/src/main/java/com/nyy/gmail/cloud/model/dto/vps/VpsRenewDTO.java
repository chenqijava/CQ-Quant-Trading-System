package com.nyy.gmail.cloud.model.dto.vps;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 购买设备
 */
@Data
public class VpsRenewDTO implements Serializable {


    /**
     * 购买数量
     */
    private List<String> batchIds;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 购买时长，月数
     */
    private Integer monthCount;


    private String userId;

    @JsonFormat(timezone ="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    private Date deadTime;


}
