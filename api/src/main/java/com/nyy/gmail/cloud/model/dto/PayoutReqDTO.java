package com.nyy.gmail.cloud.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PayoutReqDTO {
    @NotBlank(message = "商户订单号不能为空")
    private String merchantOrderNo;
    private String walletType = "TRC20";

    @NotBlank(message = "支出地址或账户不能为空")
    private String walletAddress;

    // 精确到小数点后4位
    @NotBlank(message = "金额不能为空")
    private String amount;
    @Size(max = 64, message = "备注不能超过64个字符")
    private String remark;
}
