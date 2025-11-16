package com.nyy.gmail.cloud.model.vo.vps;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;

@Data
public class VpsInfoVO {

    private Long id;


    private String vpsId;

    private String userId;

    private String accid;

    private String accName;

    private String loginStatus;

    private String runStatus;

    @JsonFormat(timezone ="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    private Date deadTime;

    private String bindStatus;

    private Integer renewTimes;

    private String description;

    private String batchId;

    private Date createTime;


}
