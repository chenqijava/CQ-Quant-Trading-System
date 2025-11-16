package com.nyy.gmail.cloud.model.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author laibao wang
 * @date 2021-09-17
 * @version 1.0
 */

@Data
@ToString
public class CommonUserEditVO implements Serializable {

    private static final long serialVersionUID = -7222468573449393069L;
    private String userID;
    private String name;
}
