package com.nyy.gmail.cloud.tasks.sieve.dto;

import com.nyy.gmail.cloud.tasks.sieve.bo.SieveActiveTaskResult;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


@Data
public class QueryTaskResponseDTO implements Serializable{
    private String userID;//商户ID
    private String mark;
    private String desc;//任务描述
    private List<String> ids;//账号数据
    private String status;//init success failed pause waitPublish
    private Integer total;//任务总数
    private Integer success;//成功数
    private Integer failed;//失败数
    private Integer unexecuteCount = 0;// 未执行数据个数
    private Integer validDataCount = 0; // 有效数据数量,筛开通
    private Long read;//已读
    private Date readTime;//阅读时间
    private Boolean isDelete;//任务组是否被删除
    private Integer publishTotalCount = 0;//需要发布的任务数量
    private Integer publishedCount = 0;//已经发布的数量
    private String publishStatus;//init success failed pause  任务发布的状态
    private List<SieveActiveTaskResult> results;          // 结果文件
    private boolean isException = false;          //是否异常订单,返回给crm
}
