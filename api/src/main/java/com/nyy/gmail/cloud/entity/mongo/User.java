package com.nyy.gmail.cloud.entity.mongo;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@ToString
@Accessors(chain = true)
@Document
public class User {
  @Id
  private String _id;
  @Version
  private Long version;

  @Indexed(unique = true)
  private String userID;//商户ID
  private String createUserID;
  private String password;
  private String newPassword;
  @Indexed(unique = true)
  private String name;
  private String status = "enable";
  private Integer loginFailedTime; // 登录失败次数
  private String role;//Role
  private String roleName;

  private BigDecimal balance = BigDecimal.ZERO;
  private String session;//用户的session，当用户的密码修改后发生改变
  private String[] customer;
  @CreatedDate
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private Date createTime;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private Date lastLoginTime;
  private String socks5Use = "admin";//指定用户使用的socks5是分配给自己的还是admin分配的
  private String socks5Area;//指定用户下账号登录IP的地区
  private String message;//紧急消息
  private Long messageCount;//紧急消息次数
  private String botId;//绑定机器人
  private String token;//绑定机器人时,tg机器人服务器验证用户信息时使用

  private boolean openApi=false;//是否开通api接口
  private String apiToken;//用户的apiToken
  private List<String>ips;//用户的ips列表,限制ip列表

  private String secret; // google auth secret

  private BigDecimal frozenBalance=BigDecimal.ZERO; // 冻结余额

  @Indexed(unique = true)
  private String userApiKey; //

  private BigDecimal totalSendBalance=BigDecimal.ZERO;
  private BigDecimal totalRechargeBalance=BigDecimal.ZERO;

  private String referrer = ""; // 每一级用,分割

  private String openRecharge="close"; // 开通充值  open / close
  private Integer referrerCount; // 推荐数量

  private BigDecimal restSendEmailCount=BigDecimal.ZERO; // 剩余发送邮件次数

  private BigDecimal totalSendEmailCount=BigDecimal.ZERO; // 总的购买次数
}
