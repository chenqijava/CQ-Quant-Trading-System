package com.nyy.gmail.cloud.model.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author wjx
 * @date 2023-06-13
 * @version 1.0
 */
@Data
@ToString
public class GraphCodeVO implements Serializable {

    private static final long serialVersionUID = 5897878201439596629L;

    /**
     * 图形验证码的key
     */
    private String graphCodeKey;

    /**
     * 图形验证码的路径
     */
    private String path;
}
