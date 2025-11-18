package com.nyy.gmail.cloud.entity.mongo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ToString
@Accessors(chain = true)
@Document
public class Role implements Serializable {

    private static final long serialVersionUID = -5306158554650933100L;
    @Id
    private String _id;
    @Version
    private Long version;
    private String userID;//商户ID
    private String name;
    private List<String> permissions;//Menu

    private Boolean isDel;
    @CreatedDate
    @JsonFormat(pattern ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;//创建时间
}
