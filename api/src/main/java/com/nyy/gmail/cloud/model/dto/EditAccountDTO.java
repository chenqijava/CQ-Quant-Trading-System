package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.io.PipedReader;
import java.io.Serializable;

@Data
public class EditAccountDTO implements Serializable {

    private String _id;
    private String groupID;
    private String remark;
}
