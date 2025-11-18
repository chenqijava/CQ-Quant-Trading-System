package com.nyy.gmail.cloud.entity.mongo;


import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@ToString
@Accessors(chain = true)
@Document
public class Region {


    /**
     * 名称
     */
    @Indexed(unique = true)
    private String name;


    /**
     * 展示名称中文
     */
    private String displayName;


    /**
     * 展示排名
     */
    @Indexed(unique = true)
    private Integer rank;


}
