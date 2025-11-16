package com.nyy.gmail.cloud.tasks.impl;

import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.SubTaskStatusEnums;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.tasks.AbstractTask;
import com.nyy.gmail.cloud.tasks.BaseTask;
import com.nyy.gmail.cloud.utils.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component("testTask")
public class TestTaskImpl extends AbstractTask implements BaseTask {

    @Autowired
    protected SubTaskRepository subTaskRepository;

    @Autowired
    protected TaskUtil taskUtil;

    @Override
    public boolean checkTask(SubTask task, Account account, Date now) {

        return false;
    }

    @Override
    public boolean runTask(SubTask task, Account account) {

        return false;
    }

    @Override
    public boolean publishTask(GroupTask groupTask) {

        // 创建SubTask, 放入消息队列中
        return true;
    }

    @Override
    protected SubTaskRepository getSubTaskRepository() {
        return subTaskRepository;
    }

    @Override
    protected TaskUtil getTaskUtil() {
        return taskUtil;
    }

    @Override
    public SubTask reportTaskStatus(SubTask wt, SubTaskStatusEnums status, Object result) {
        return wt;
    }
}
