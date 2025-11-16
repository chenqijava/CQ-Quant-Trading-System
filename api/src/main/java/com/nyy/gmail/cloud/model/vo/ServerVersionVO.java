package com.nyy.gmail.cloud.model.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;

/**
 * @author laibao wang
 * @date 2021-09-17
 * @version 1.0
 */

@Data
@ToString
public class ServerVersionVO implements Serializable {

    private static final long serialVersionUID = 198618225055725559L;
    private String server;
    private String gateway;
}
