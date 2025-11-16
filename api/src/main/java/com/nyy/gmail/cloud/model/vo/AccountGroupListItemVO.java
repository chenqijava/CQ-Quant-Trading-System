package com.nyy.gmail.cloud.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class AccountGroupListItemVO {

    private String _id;

    private Long version;

    private String userID;

    private String groupName;

    private Date createTime;

    private String groupType;

    private Integer accountNum;

}
