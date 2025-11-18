package com.nyy.gmail.cloud.entity.mongo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;


@Data
@ToString
@Document
@Accessors(chain = true)
public class Params implements Serializable{

    private static final long serialVersionUID = -6477608939331193005L;
    @Id
    private String _id;
    @Version
    private Long version;
    private String userID;//商户ID
    private String name;
    private String code;
    private Object value;
    @CreatedDate
    @JsonFormat( pattern ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;
}
