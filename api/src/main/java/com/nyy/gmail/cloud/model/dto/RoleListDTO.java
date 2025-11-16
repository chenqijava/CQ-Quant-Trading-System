package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Map;

@Data
@ToString
@Accessors(chain = true)
public class RoleListDTO implements Serializable {

    private static final long serialVersionUID = -8961861977486095810L;
    private String name;
    private String userID;
    private Map<String, String> filters;
}
