package com.nyy.gmail.cloud.entity.mongo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Data
@ToString
@Document
@Accessors(chain = true)
@CompoundIndexes({
        @CompoundIndex(name = "vpsCount_lastUseTime_idx", def = "{'vpsCount': 1, 'lastUseTime': 1, 'areaCode': 1}"),
        @CompoundIndex(def="{'ip':1,'port':1,'username':1}", unique = true)
})
public class Socks5 implements Serializable{

    private static final long serialVersionUID = -231223193987775242L;
    @Id
    private String _id;
    @Version
    private Long version;
    private String userID;//商户ID
    private String desc;//描述
    private String proxyAccount;//代理账号 ProxyAccount
    private String platform;//平台
    private String ip;//ip
    private int port;//端口
    private String username;//用户名
    private String password;//密码
    private String areaCode;//地区
    private String proxyIp;//代理到的ip地址
    private String area;//地区名称 Socks5IpArea
    private String countryName;//地区名称 Socks5IpCountryName
    private String batchid;//批次号
    private String status;//状态  init/error/connectionError(发送网关请求时报错)
    private String ipCheckStatus;//状态  init/error/connectionError(发送网关请求时报错)
    private int statusFlag; // 状态
    private long useCount = 0;//使用次数（每登录一次加一）
    private String belongUser;//分配给哪个用户使用
    private List<String> belongVps;//用户下哪些设备使用
    private long vpsCount = 0;//已绑设备数量
    @CreatedDate
    @JsonFormat( pattern ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createTime;//创建时间
    @JsonFormat( pattern ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date lastUseTime;//最后使用时间
    @JsonFormat( pattern ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date getdnsTime;//最后调用getdns的时间
    private String proxy;//调用getdns接口后得到的字符串（base64）,以相同的参数重复调用接口，proxy一样
    private Map shortIps;//调用getdns接口后得到的json格式组成的服务器地址列表。选取：short.weixin.qq.com 节点下的其中一个IP登录
    private String proxyA16;//调用getdns接口后得到的字符串（base64）,以相同的参数重复调用接口，proxy一样
    private Map shortIpsA16;//调用getdns接口后得到的json格式组成的服务器地址列表。选取：short.weixin.qq.com 节点下的其中一个IP登录
    private long lastCheckTime;//最后一次检查是否有网络的时间
    private long lastCheckNormalTime;//最后一次有网的时间
    private long bannedAccountCount;//被封账号统计
    private long switchTimes;//切换ip次数
    @LastModifiedDate
    @JsonFormat( pattern ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date updateTime;
}
