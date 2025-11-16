package com.nyy.gmail.cloud.model.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: wjx
 * @Date: 2024/12/12 09:41
 */
@Data
@ToString
public class PhoneRuleVO implements Serializable {
    private String code;

    // 手机号位数
    private List<Integer> allowedNumbers;

    // 地区
    private List<String> regions;
}
