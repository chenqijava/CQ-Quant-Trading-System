package com.nyy.gmail.cloud.model.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author laibao wang
 * @date 2021-09-17
 * @version 1.0
 */

@Data
@ToString
public class RoleDeleteVO implements Serializable {

    private static final long serialVersionUID = 7684034912505662773L;
    Map failed;
    List<String> failedType;
}
