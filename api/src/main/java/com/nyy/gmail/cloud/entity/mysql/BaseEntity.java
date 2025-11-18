package com.nyy.gmail.cloud.entity.mysql;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import lombok.Data;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    // 和mongodb的_id保持一致
    @Id
    @Column(name = "_id", columnDefinition = "char(30) NOT NULL comment 'ID'")
    private String _id;

    // 乐观锁
    @Version
    @Column(columnDefinition = "bigint(20) NOT NULL comment '乐观锁'")
    private Long version;
    
    @CreatedDate
    @Column(columnDefinition = "datetime comment '创建时间'")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;
    
    @LastModifiedDate
    @Column(columnDefinition = "datetime comment '更新时间'")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date updateTime;

    @PrePersist
    public void prePersist() {
        if (this._id == null) {
            this._id = new ObjectId().toHexString();
        }
    }
}
