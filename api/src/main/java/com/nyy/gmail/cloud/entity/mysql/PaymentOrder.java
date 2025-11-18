package com.nyy.gmail.cloud.entity.mysql;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "payment_order")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 100)
    @Column(name = "order_no", length = 100)
    private String orderNo;

    @Size(max = 100)
    @Column(name = "channel_order_no", length = 100)
    private String channelOrderNo;

    @Column(name = "amount",precision = 20,scale = 4)
    private BigDecimal amount;

    @Size(max = 50)
    @Column(name = "wallet_type", length = 50)
    private String walletType;

    @Size(max = 50)
    @Column(name = "user_id", length = 50)
    private String userId;

    @Size(max = 100)
    @Column(name = "wallet_id", length = 100)
    private String walletId;

    @Size(max = 200)
    @Column(name = "remark", length = 200)
    private String remark;

    @Size(max = 50)
    @Column(name = "status", length = 50)
    private String status;

    @Size(max = 300)
    @Column(name = "address", length = 300)
    private String address;

    @Size(max = 300)
    @Column(name = "res_msg", length = 300)
    private String resMg;

    @Column(name = "time_out_millis")
    private Long timeOutMillis;

    @CreatedDate
    @Column(columnDefinition = "datetime comment '创建时间'")
    private Date createTime;

    @LastModifiedDate
    @Column(columnDefinition = "datetime comment '更新时间'")
    private Date updateTime;

    @Column(name = "sendAmount",precision = 20,scale = 4)
    private BigDecimal sendAmount;
}
