package com.nyy.gmail.cloud.tasks.sieve.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nyy.gmail.cloud.utils.JacksonMapUtils;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class SieveActiveSubTaskParams {
    private String project;
    private String dataFilePath;
    private Long dataTotal;
    private Long total = 0L;
    private Long success = 0L;
    private Long failed = 0L;
    private Integer unexecuteCount = 0;// 未执行数据个数
    private Integer validDataCount = 0; // 有效数据数量,筛开通
    private boolean isException = false;
    private Integer index;

    private String groupTaskId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date startTime;

    private List<SieveActiveTaskResult> results;

    @SuppressWarnings("rawtypes")
    public Map toMap() {
        return JacksonMapUtils.toMap(this);
    }
}
