package com.nyy.gmail.cloud.model.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

import com.nyy.gmail.cloud.entity.mongo.Menu;

/**
 * @author laibao wang
 * @date 2021-09-17
 * @version 1.0
 */

@Data
@ToString
public class CommonUserPermissionsVO implements Serializable {

    private static final long serialVersionUID = -5112679137524318516L;
    private List<Menu> permissions;
}
