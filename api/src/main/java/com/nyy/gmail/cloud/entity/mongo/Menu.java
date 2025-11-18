package com.nyy.gmail.cloud.entity.mongo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nyy.gmail.cloud.common.MenuType;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;


@Data
@ToString
@Accessors(chain = true)
@Document
@CompoundIndexes({
        @CompoundIndex(def="{'key':1,'parent':1}")
})
public class Menu implements Serializable{

    private static final long serialVersionUID = -4705693706502713537L;
    @Id
    private String _id;
    @Version
    private Long version;
    private long index;//菜单的顺序，publish启动时会根据lib/menus.js中的数据更新
    private MenuType key;//菜单的key要唯一，publish启动时会根据key更新菜单数据，更改时要同时更改数据库和lib/menus.js
    private String url;
    private String parent;//父菜单的_id，一级菜单没有parent
    private String name;//菜单名称，publish启动时会根据lib/menus.js中的数据更新
    private Map<String, Object> icon;//菜单的图标，publish启动时会根据lib/menus.js中的数据更新
    private String type;// menu, button
    @LastModifiedDate
    @JsonFormat(pattern ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date updateTime;
    @CreatedDate
    @JsonFormat(pattern ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;
}
