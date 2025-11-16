package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

@Data
@ToString
@Accessors(chain = true)
public class Params implements Serializable {
    private static final long serialVersionUID = 7926218311659440612L;

    private HashMap<String, Object> filters;
    private String onlineStatus;
    private HashMap<String, Integer> sorter;
    private Boolean unBanned;
    private Boolean userAdmin;
    private HashMap<String, Object> data;
    private List<String> selectedRowKeys;
    private String selectedRowKey;
}
