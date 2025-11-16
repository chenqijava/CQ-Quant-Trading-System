package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

@Data
@ToString
@Accessors(chain = true)
public class ProxyAccountListDTO implements Serializable {

    private static final long serialVersionUID = -967114230116309305L;
    private String userID;
    private String desc;
    private String platform;
    private String token;
    private Map<String,String> filters;
}
