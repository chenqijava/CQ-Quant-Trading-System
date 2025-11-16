package com.nyy.gmail.cloud.tasks;

import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.SubTaskStatusEnums;

import java.util.Date;

public interface Task {
    boolean checkTask(SubTask task, Account account, Date now);

    boolean runTask(SubTask task, Account account);

    SubTask reportTaskStatus(SubTask wt, SubTaskStatusEnums status, Object result);
}
