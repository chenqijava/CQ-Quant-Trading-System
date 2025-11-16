package com.nyy.gmail.cloud.model.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 用户绑定tg使用token和绑定的bot的username
 * @author wjx
 * @date 2023-06-13
 * @version 1.0
 */
@Data
@ToString
public class UserTokenVO implements Serializable {
    private static final long serialVersionUID = 7771672563811483121L;

    private String userID;
    private String token;
    private String botUsername;
}
