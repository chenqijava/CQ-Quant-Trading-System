package com.nyy.gmail.cloud.tasks.sieve.bo;

import com.nyy.gmail.cloud.tasks.sieve.enums.SieveActiveTaskResultTypeEnum;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
public class SieveActiveTaskResult {
    private SieveActiveTaskResultTypeEnum type;
    private String name;//文件名称
    private String filepath;//文件路径
    private Integer count = 0;//总数据行数
}
