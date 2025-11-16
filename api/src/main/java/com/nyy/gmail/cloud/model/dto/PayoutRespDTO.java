package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PayoutRespDTO {
    private String orderNo;
    private String merchantOrderNo;
    private String amount;
    private String status;
    private String fee;
}
