package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @author laibao wang
 * @date 2021-09-16
 * @version 1.0
 */

@Data
@ToString
@Accessors(chain = true)
public class UserEditPwdDTO implements Serializable {

    private static final long serialVersionUID = 8692170022142831710L;
    private String password;
    private List<String> ids;
}
