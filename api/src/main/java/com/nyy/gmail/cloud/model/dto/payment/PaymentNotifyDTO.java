package com.nyy.gmail.cloud.model.dto.payment;

import lombok.Data;

@Data
public class PaymentNotifyDTO {

    private String merchant_trade_no;

    private String order_status;

    private String amount;

    private String order_no;

    private Long pay_timestamp;

    private String sign_type;

    private String sign;
}
