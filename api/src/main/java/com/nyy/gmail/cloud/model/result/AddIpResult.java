package com.nyy.gmail.cloud.model.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class AddIpResult implements Serializable {

    /**
     * 总条数
     */
    private Integer total = 0;

    /**
     * 成功的条数
     */
    private Integer success = 0;

    /**
     * 重复的条数
     */
    private Integer repeat = 0;

    /**
     * 格式错误的条数
     */
    private Integer error = 0;


}
