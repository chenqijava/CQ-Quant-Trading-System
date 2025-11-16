package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@ToString
@Accessors(chain = true)
public class Socks5ListDTO implements Serializable {

    private static final long serialVersionUID = -6778336279383726848L;
    private String batchid;
    private String ip;
    private long port;
    private String username;
    private Boolean userAdmin;
    private String password;
    private String platform;
    private String areaCode;
    private List<String> belongVps;
    private String belongUser;
    private String userID;
    private Map<String,Object> filters;
    private Map<String,Integer> sorter;
}
