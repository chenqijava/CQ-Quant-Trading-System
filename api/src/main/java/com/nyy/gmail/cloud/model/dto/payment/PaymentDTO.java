package com.nyy.gmail.cloud.model.dto.payment;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDTO {

    private BigDecimal amount;
}
