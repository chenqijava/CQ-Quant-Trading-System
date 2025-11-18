package com.nyy.gmail.cloud.entity.mongo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;


@Data
@ToString
@Document
@Accessors(chain = true)
public class ProxyAccount implements Serializable{

    private static final long serialVersionUID = -5306158554650933100L;
    @Id
    private String _id; //? 签名
    @Version
    private Long version;
    private String userID;//商户ID
    private String desc;//账号备注
    private String platform;//代理平台  he / leo
    private String token;//用户id
    private String id;
    private String account;
    private String protocol;
    private Boolean enable;
    private long maxVpsNum;
    @CreatedDate
    @JsonFormat( pattern ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;//创建时间
}
