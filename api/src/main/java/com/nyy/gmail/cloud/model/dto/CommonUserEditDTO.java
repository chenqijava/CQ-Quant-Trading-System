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
public class CommonUserEditDTO implements Serializable {

    private static final long serialVersionUID = 1839272818297025576L;
    private String userID;
    private String password;
    private String newPassword;
    private String name;
    private String newName;
}
