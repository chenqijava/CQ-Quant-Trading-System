package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@ToString
@Accessors(chain = true)
public class GraphCodeDTO implements Serializable {

    private static final long serialVersionUID = 5647269701791927451L;
    /**
    * 图形验证码
    */
    private String graphCode;

    /**
    * 图形验证码的key
    */
    private String graphCodeKey;

    /**
     * 图形验证码的路径
     */
    private String path;

}
