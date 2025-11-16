package com.nyy.gmail.cloud.tasks.sieve.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 给主控,或者第三方接口发送任务参数
 */
@Data
@ToString
@Accessors(chain = true)
public class OpTaskDTO implements Serializable {
    private static final long serialVersionUID = 2991160549244497505L;
    private String userID;
    private String groupTaskId;
    private List<String> groupTaskIds;
    private Integer priority = 0;   // 优先级 ,默认普通 -1,为延迟,0为普通,1为优先,2为紧急,客户订单优于内部订单
    private String reason; // 任务原因,用于记录日志

}
