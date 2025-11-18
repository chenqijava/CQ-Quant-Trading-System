package com.nyy.gmail.cloud.entity.mysql;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Date;

@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "cloud_friend",uniqueConstraints = {
        @UniqueConstraint(name = "uk_friend", columnNames = {"accId", "uid"})
})
public class Friend extends BaseEntity {
    @Column(columnDefinition = "varchar(50) NOT NULL comment '用户ID'")
    private String userID;

    @Column(columnDefinition = "varchar(50) NOT NULL comment 'accid'")
    private String accId;

    @Column(columnDefinition = "varchar(50) NOT NULL comment 'accID'")
    private String uid;

    @Column(columnDefinition = "varchar(50) NOT NULL comment 'phone'")
    private String phone;

    @Column(columnDefinition = "varchar(50) NOT NULL comment '聊天ID'")
    private String chatId;

    @Column(columnDefinition = "varchar(500) NOT NULL comment '备注'")
    private String remark;

    @Column(columnDefinition = "int NOT NULL comment '未读消息数量' default 0")
    private Long unreadMessageCount;

    @Column(columnDefinition = "varchar(500) NOT NULL comment '最后一条消息' default ''")
    private String lastMessage;

    @Column(columnDefinition = "datetime comment '最后一条消息时间'")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date lastMessageTime;

    @Column(columnDefinition = "varchar(500) NOT NULL comment '头像'")
    private String avatar;

    @Column(columnDefinition = "datetime comment '最后一条消息真实的时间'")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date realLastMessageTime;

    public void setLastMessage(String content) {
        this.lastMessage = content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
