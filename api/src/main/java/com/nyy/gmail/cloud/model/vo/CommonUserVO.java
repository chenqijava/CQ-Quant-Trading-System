package com.nyy.gmail.cloud.model.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author laibao wang
 * @date 2021-09-17
 * @version 1.0
 */

@Data
@ToString
public class CommonUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5379134341801010257L;
    private String userID;
    private String name;
    private long group;
    private long label;
    private String[] customer;
    private ServerVersionVO serverVersion;
    private boolean setSecretKey;
    private BigDecimal balance;
    private BigDecimal frozenBalance;
    private String userApiKey;
    private String role;

    private String openRecharge;

    private BigDecimal restSendEmailCount;

    private String biz;
}
