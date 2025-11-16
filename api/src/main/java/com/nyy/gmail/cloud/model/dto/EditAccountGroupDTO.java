package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class EditAccountGroupDTO implements Serializable {

    private List<String> ids;
    private String groupID;
}
