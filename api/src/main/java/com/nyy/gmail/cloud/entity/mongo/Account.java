package com.nyy.gmail.cloud.entity.mongo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Document
@Data
@ToString
@Accessors(chain = true)
public class Account implements Serializable {

    private static final long serialVersionUID = 3431180356617110574L;

    @Id
    private String _id;
    @Version
    private Long version;

    @Indexed
    private String userID; // 用户ID

    @Indexed
    private String groupID; // 分组ID

    private String groupName;

    private Boolean changeSocks5; // 是否改变socks

    @Indexed(unique = true)
    private String accID; // 唯一ID

    private String alias; // 别名

    private String username; // 用户名

    private String nickname; // 昵称

    private String password; // 密码

    private String session; // session

    private String loginSession; // session

    private String email; // email

    private String phone; // 手机号

    @Indexed
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;

    @Indexed
    private String onlineStatus = "0"; // 账号状态（-1/0/1/2，被封/离线()/上线/等待上线/）

    private String socks5Id; // socks5 ID

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date changeOnlineStatusTime;

    @Indexed
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date lastSendMsgTime;


    private String loginError;

    /**
     * 备注
     */
    private String remark;

    private String app;
    private String lstBindingKeyAlias;
    private String googleAccountDataStore;
    private String deviceinfo;
    private String device;
    private String token;

    private String outlookGraphClientId;
    private String outlookGraphRefreshToken;

    private String proxyIp;
    private String proxyPort;
    private String proxyUsername;
    private String proxyPassword;

    private Integer used = 0;
    private List<String> usedPlatformIds;

    private List<String> realUsedPlatformIds; // 获取过邮件的

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date lastPullMessageTime;

    private Boolean isCheck = false;

    private String cookie;

    // 重新检测次数
    private Integer retryCheckNum = 0;

    private Integer sendEmailNumByDay = 0;

    private Integer sendEmailNumByDayDisplay = 0;

    private Boolean limitSendEmail = false;

    private String type = "mobile"; // mobile/web/sendgrid

    private String sendGridApiKey;

    private Integer sendEmailTotal = 0; // 累计发送次数

    private String openExportReceiveCode="0"; // 1 开启导出接码 0 不开启

    private String workspaceCredentialJSON;

    private String workspaceClientId;

    private String workspaceClientSecret;

    private String workspaceRefreshToken;

    private String smtpHost;
    private String smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String imapHost;
    private String imapPort;
    private boolean imapSsl=true;
    private String deviceToken;
}
