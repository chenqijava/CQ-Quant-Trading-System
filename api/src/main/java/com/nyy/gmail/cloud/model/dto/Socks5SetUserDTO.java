package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@ToString
@Accessors(chain = true)
public class Socks5SetUserDTO implements Serializable {

    private static final long serialVersionUID = -4032347391909747488L;
    List<String> ids;
    private String userID;
}
