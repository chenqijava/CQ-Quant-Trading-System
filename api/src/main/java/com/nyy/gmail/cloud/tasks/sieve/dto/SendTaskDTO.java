package com.nyy.gmail.cloud.tasks.sieve.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

/**
 * 给主控,或者第三方接口发送任务参数
 */
@Data
@ToString
@Accessors(chain = true)
public class SendTaskDTO implements Serializable {
    private static final long serialVersionUID = 2991160549244497505L;

    private String userID;                      //账号id
    private String project;                     // 订单项目类型 'tg' 'ws'
    private String type;                        // 订单类型
    private String taskName;                     //任务名称
    private String orderId;                      // 订单ID
    private String dataType;                     // 数量类型
    private Integer beforeDaysActive;            // 活跃天数
    private String identify;                     // 分析头像 false:不分析 true:分析,all:全部
    private String dataFile;                     // 数据文件相对于相对路径,(相对于resPath路径)
    private String dataFileName;                 // 数据文件名称(发结果文件时,使用)
    private Integer total;                       //手机号总数
    private File file;                           // 数据文件
    private Integer priority = 0;   // 优先级 ,默认普通 -1,为延迟,0为普通,1为优先,2为紧急,客户订单优于内部订单
    private Boolean exportBusiness = false;//是否商业号,账号状态(类似个人简介),ws会在后面加两列,
    private Boolean exportRace = false;//为true时,ws和tg都会在age后面一列加人种列, tg还会在最后两列加上first_name,last_name
    private Boolean recentlyNotActive = false;//  隐私数据是否属于活跃数据,默认false,属于活跃数据,为true时,属于不活跃数据
    Map<String,String> extraParams;      // 其他参数,可以传入任何参数,如:{"key":"value"}等,用于第三方接口传参
    private Boolean exportDeleted = false;//是否冻结的列(是/否)

}
