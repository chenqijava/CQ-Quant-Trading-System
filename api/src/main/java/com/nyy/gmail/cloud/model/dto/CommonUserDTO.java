package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author laibao wang
 * @date 2021-09-16
 * @version 1.0
 */

@Data
@ToString
@Accessors(chain = true)
public class CommonUserDTO implements Serializable {

    private static final long serialVersionUID = 4936351648413738120L;
    private String userID;
    private String password;
    // 应该带上codeKey 用于校验code与codeKey是否匹配
    private String graphCodeKey;
    private String code;
}
