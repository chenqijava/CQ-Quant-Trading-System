package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 冻结用户DTO
 */

@Data
@ToString
@Accessors(chain = true)
public class UserDisabledDTO implements Serializable {

    /**
     * 用户ids
     */
    private List<String> ids;
}
