package com.nyy.gmail.cloud.entity.mysql;


import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Date;

@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "cloud_notice_record")
public class NoticeRecord extends BaseEntity {

    @Column(columnDefinition = "varchar(50) NOT NULL comment '用户ID'")
    private String userID;

    @Column(columnDefinition = "varchar(50) NOT NULL comment '消息ID'")
    private String messageId;

    @Column(columnDefinition = "varchar(50) NOT NULL comment '状态 PROCESSING\n" +
            "SUCCESS\n" +
            "FAILURE'")
    private String status;

    @Column(columnDefinition = "varchar(255) NOT NULL default '' comment '通知地址'")
    private String callbackUrl;

    @Column(columnDefinition = "varchar(50) NOT NULL default '' comment '通知间隔时间 1m  5m 10m 30m'")
    private String timeInterval;

    @Column(columnDefinition = "varchar(255) NULL default '' comment '返回结果'")
    private String result;

    @Column(columnDefinition = "datetime default '1970-01-01 01:00:00' comment '执行时间'")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date execTime;

    @Column(columnDefinition = "varchar(50) NOT NULL default '' comment 'APIKEY ID'")
    private String apiKeyId;
}
