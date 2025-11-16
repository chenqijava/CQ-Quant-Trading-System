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
public class ProxyAccountChangeEnableDTO implements Serializable {

    private static final long serialVersionUID = 6618340658932941652L;
    private Boolean enable;
}
