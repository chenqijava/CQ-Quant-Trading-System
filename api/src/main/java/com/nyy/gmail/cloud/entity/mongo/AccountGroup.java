package com.nyy.gmail.cloud.entity.mongo;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.nyy.gmail.cloud.enums.AccountGroupTypeEnums;
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
@Accessors(chain = true)
@CompoundIndexes({
        @CompoundIndex(def = "{'userID':1,'type':1,'status':1,'finishTime':1,'isDelete':1}"),
        @CompoundIndex(def = "{'publishStatus':1}"),
        @CompoundIndex(def = "{'platformTaskId':1}"),
        @CompoundIndex(def = "{'userID':1, 'groupName':1}", unique = true)
})
@Document
public class AccountGroup implements Serializable {

    private static final long serialVersionUID = -5057244223970164818L;

    @Id
    private String _id;
    @Version
    private Long version;

    private String userID;

    private String groupName;

    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;

    /**
     * 分组类型（普通分组，默认分组）
     *
     * @see AccountGroupTypeEnums
     */
    private String groupType;

    /**
     * 账号数量
     */
    private Long accountNum;
    /**
     * 在线账号数量
     */
    private Long accountOnlineNum;

}
