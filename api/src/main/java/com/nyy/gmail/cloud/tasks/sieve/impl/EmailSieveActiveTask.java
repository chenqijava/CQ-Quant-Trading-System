package com.nyy.gmail.cloud.tasks.sieve.impl;

import com.nyy.gmail.cloud.tasks.sieve.AbstractSieveActiveTask;
import com.nyy.gmail.cloud.tasks.sieve.enums.SieveActiveTaskResultTypeEnum;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class EmailSieveActiveTask extends AbstractSieveActiveTask {
    private static final List<SieveActiveTaskResultTypeEnum> taskResultTypeEnums = List.of(
            SieveActiveTaskResultTypeEnum.success,
            SieveActiveTaskResultTypeEnum.failed,
            SieveActiveTaskResultTypeEnum.unexecute,
            SieveActiveTaskResultTypeEnum.unknown,
            SieveActiveTaskResultTypeEnum.forbidden
    );

    @Override
    protected List<SieveActiveTaskResultTypeEnum> getTaskResultTypes() {
        return taskResultTypeEnums;
    }
}
