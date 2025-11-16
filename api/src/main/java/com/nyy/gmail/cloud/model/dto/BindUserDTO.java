package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 绑定tg账号,使用的参数
 */

@Data
@ToString
@Accessors(chain = true)
public class BindUserDTO implements Serializable {

    private static final long serialVersionUID = 4936351648413738120L;
    private String token;
    private String name;
    private String username;
    private String accID;
}
