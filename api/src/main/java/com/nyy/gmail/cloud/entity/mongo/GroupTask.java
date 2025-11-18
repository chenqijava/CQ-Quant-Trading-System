package com.nyy.gmail.cloud.entity.mongo;

import com.alibaba.fastjson2.JSON;
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
import java.util.List;
import java.util.Map;

@Data
@ToString
@Accessors(chain = true)
@CompoundIndexes({
        @CompoundIndex(def="{'userID':1,'type':1,'status':1,'finishTime':1,'isDelete':1}"),
        @CompoundIndex(def="{'type':1,'status':1}"),
        @CompoundIndex(def="{'publishStatus':1}"),
        @CompoundIndex(def="{'platformTaskId':1}")
})
@Document
public class GroupTask implements Serializable {

    private static final long serialVersionUID = -3941556535092017791L;

    @Id
    private String _id;
    @Version
    private Long version;

    private String userID;

    private String mark;

    private String desc;

    private List<String> ids;

    private String type;

    private String platformTaskId;

    private String status; // waitPublish -> processing -> init -> pause/success/failed

    private String userAction; // 用户操作 finish(停止) forceFinish(强制停止) restart(重启)

    private Map params;

    private Map result;

    private Boolean needFeedback = false;

    private Boolean needFeedback2VideoSpider = false;

    private String executeType;

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

    private Long total = 0L;

    private Long success = 0L;

    private Long failed = 0L;

    private Long read;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date readTime;

    private Long show;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date showTime;

    @Indexed
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date lastShowTime;

    private Boolean isDelete = false;

    private Long publishTotalCount = 0L;

    @Indexed
    private Long publishedCount = 0L;

    private String publishStatus; // init success failed pause  任务发布的状态

    private String userUUID;

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
