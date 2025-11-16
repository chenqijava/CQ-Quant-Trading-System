package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

@Data
@ToString
@Accessors(chain = true)
public class UserListDTO implements Serializable {

    private static final long serialVersionUID = -1607646032471128102L;
    private String name;
    private String userID;
    private String createUserID;
    private Boolean openApi;
    private Map<String,String> filters;
}
