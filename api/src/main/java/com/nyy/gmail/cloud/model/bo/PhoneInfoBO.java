package com.nyy.gmail.cloud.model.bo;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 手机号信息,包含区号和国家名称
 */
@Data
@ToString
@Accessors(chain = true)
public class PhoneInfoBO {
    private String areaCode;
    private String countryName;
}