package com.nyy.gmail.cloud.tasks.sieve.dto;

import com.nyy.gmail.cloud.tasks.sieve.enums.SieveActiveTaskResultTypeEnum;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * 给主控,或者第三方接口发送任务参数
 */
@Data
@ToString
@Accessors(chain = true)
public class DownloadTaskDTO implements Serializable {
    private static final long serialVersionUID = 2991160549244497505L;

    private SieveActiveTaskResultTypeEnum type;
    private String groupTaskId;
    private Path parentPath;
    private String filepath;
    private String dataFilepath;// 数据文件

}
