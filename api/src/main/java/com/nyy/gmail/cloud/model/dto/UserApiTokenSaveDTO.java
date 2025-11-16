package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@ToString
@Accessors(chain = true)
public class UserApiTokenSaveDTO implements Serializable {

    private static final long serialVersionUID = -1607646032471128102L;
    private String userID;
    private Boolean openApi;
    private List<String> ids;
    private List<String> ips;
}
