package com.nyy.gmail.cloud.model.result;

import lombok.Data;

@Data
public class ChannelPaymentResult {

    private String orderNo;

    private String address;

    private String amount;

    private String merchantOrderNo;

    private String status;

    private Long timeoutMillis;
}
