package com.nyy.gmail.cloud.model.vo.payment;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentOrderVO {

    private String orderNo;

    private String walletType;

    private String address;

    private String amount;

    private String qrCode;

    private Long deadTime;
}
