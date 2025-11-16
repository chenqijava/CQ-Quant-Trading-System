package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;


@Data
@ToString
@Accessors(chain = true)
public class UserIDDTO implements Serializable {
    private static final long serialVersionUID = -2682703318667207524L;
    private String _id;
    private String userID;
}
