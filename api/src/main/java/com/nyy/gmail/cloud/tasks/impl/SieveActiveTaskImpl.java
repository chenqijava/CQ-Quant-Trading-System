package com.nyy.gmail.cloud.tasks.impl;

import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.tasks.AbstractTask;
import com.nyy.gmail.cloud.tasks.BaseTask;
import com.nyy.gmail.cloud.tasks.sieve.SieveActiveTaskFactory;
import com.nyy.gmail.cloud.utils.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("SieveActive")
public class SieveActiveTaskImpl extends AbstractTask implements BaseTask {

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private SieveActiveTaskFactory sieveActiveTaskFactory;

    @Override
    protected SubTaskRepository getSubTaskRepository() {
        return subTaskRepository;
    }

    @Override
    protected TaskUtil getTaskUtil() {
        return taskUtil;
    }

    @Override
    public boolean publishTask(GroupTask groupTask) {
        sieveActiveTaskFactory.getTaskBean(groupTask).publishTask(groupTask);
        return true;
    }

    @Override
    public boolean checkTask(SubTask task, Account account, Date now) {
        return true;
    }

    @Override
    public boolean runTask(SubTask task, Account account) {
        return false;
    }
}
