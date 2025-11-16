package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 解冻用户DTO
 */

@Data
@ToString
@Accessors(chain = true)
public class UserEnableDTO implements Serializable {

    /**
     * 用户ids
     */
    private List<String> ids;
}
