package com.nyy.gmail.cloud.entity.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "cloud_api_key",uniqueConstraints = {
        @UniqueConstraint(name = "uk_apiKey", columnNames = {"apiKey"})
})
public class ApiKey extends BaseEntity {

    @Column(columnDefinition = "varchar(50) NOT NULL comment '用户ID'")
    private String userID;

    @Column(columnDefinition = "varchar(255) NOT NULL comment 'API KEY'", unique = true)
    private String apiKey;

    @Column(columnDefinition = "varchar(255) NOT NULL comment 'API Secret'")
    private String apiSecret;

    @Column(columnDefinition = "varchar(255) NOT NULL comment 'IP白名单，多个逗号分隔'")
    private String whiteIp;

    @Column(columnDefinition = "varchar(255) NOT NULL comment '收到短信回调'")
    private String receiveCallbackUrl;

}
