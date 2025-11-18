package com.nyy.gmail.cloud.entity.mongo;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.nyy.gmail.cloud.utils.JacksonMapUtils;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
@ToString
@Accessors(chain = true)
@CompoundIndexes({
        @CompoundIndex(def="{'groupTaskId':1,'status':1,'accid':1}"),
        @CompoundIndex(def="{'userID':1,'type':1,'status':1,'accid':1}"),
        @CompoundIndex(def="{'status':1}"),
        @CompoundIndex(def="{'type':1,'status':1,'accid':1}"),
})
@Document
public class SubTask implements Serializable {

    private static final long serialVersionUID = -2850193171699897438L;

    @Id
    private String _id;
    @Version
    private Long version;

    private String userID;

    private String groupTaskId;

    private String accid;

    @Indexed
    private String type;

    private String status;

    private Map checkParams;

    private Map params;

    private Map result;

    private Boolean needFeedback = false;

    private String executeType;

    private String tmpId;

    @Indexed
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date executeTime;

    @Indexed
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;

    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date finishTime;

    public void setParamsBean(Object obj) {
        if (obj == null) {
            this.params = null;
        } else {
            this.params = JacksonMapUtils.toMap(obj);
        }
    }

    @Transient
    public <T> T getParamBean(Class<T> clazz) {
        if (this.params == null) {
            return null;
        }
        return JacksonMapUtils.fromMap(this.params, clazz);
    }
}
