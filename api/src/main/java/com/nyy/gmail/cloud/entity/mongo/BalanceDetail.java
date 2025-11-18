package com.nyy.gmail.cloud.entity.mongo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;

@Data
@ToString
@Accessors(chain = true)
@Document
public class BalanceDetail {

    private String operator;

    private String userID;

    private String name;

    private String expenseType;

    private String type;

    private BigDecimal value = BigDecimal.ZERO;

    private BigDecimal balance = BigDecimal.ZERO;

    @TextIndexed
    private String description;

    private String orderId;

    @Indexed
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;

    private String emailOrderNo; // 关联邮箱订单编号
}
