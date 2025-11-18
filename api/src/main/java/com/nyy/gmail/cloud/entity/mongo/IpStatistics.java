package com.nyy.gmail.cloud.entity.mongo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;


@Data
@ToString
@Accessors(chain = true)
@Document
public class IpStatistics implements Serializable{

    private static final long serialVersionUID = 7222830045767657864L;
    @Id
    private String _id;
    @Version
    private Long version;
    private String userID;//商户ID
    private String ip;
    private long loginNumber = 1;
    @LastModifiedDate
    @JsonFormat( pattern ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date lastLoginTime = new Date();
}
