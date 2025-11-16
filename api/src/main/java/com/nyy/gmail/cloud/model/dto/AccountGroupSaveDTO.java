package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccountGroupSaveDTO implements Serializable {

    private String id;

    private String groupName;

}
