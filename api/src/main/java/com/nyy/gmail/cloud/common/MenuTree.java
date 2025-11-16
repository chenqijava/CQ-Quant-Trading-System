package com.nyy.gmail.cloud.common;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MenuTree implements TreeEntity<MenuTree> {
    public MenuType key;
    public String name;
    public String url;
    public Map icon;
    public List<MenuTree> childList;
    public String type;
}
