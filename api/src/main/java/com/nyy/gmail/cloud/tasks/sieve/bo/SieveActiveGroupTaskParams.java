package com.nyy.gmail.cloud.tasks.sieve.bo;

import com.nyy.gmail.cloud.utils.JacksonMapUtils;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class SieveActiveGroupTaskParams {
    private String project;
    private String taskDesc;
    private Integer publishTotalCount;
    private String addMethod;
    private String dataFilePath;

    private Long unexecuteCount;
    private Long validDataCount;

    private List<String> results;

    @SuppressWarnings("rawtypes")
    public Map toMap() {
        return JacksonMapUtils.toMap(this);
    }
}
