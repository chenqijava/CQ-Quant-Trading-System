package com.nyy.gmail.cloud.tasks.impl.cron;

import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.CronTaskTypesEnums;
import com.nyy.gmail.cloud.enums.SubTaskStatusEnums;
import com.nyy.gmail.cloud.framework.TaskSchedulingService;
import com.nyy.gmail.cloud.framework.dto.AddTaskDomainDto;
import com.nyy.gmail.cloud.tasks.CronBaseTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("CheckAccountTCPConnection")
public class CheckAccountTCPConnectionTaskImpl implements CronBaseTask {

    private static final Map<String, Long> checkingAccountTCPConnectionTime = new HashMap<>();

    private static final Map<String, Boolean> checkingAccountTCPConnection = new HashMap<>();

    @Autowired
    private TaskSchedulingService taskSchedulingService;

    @Override
    public boolean checkTask(SubTask task, Account account, Date now) {
        return true;
    }

    @Override
    public boolean runTask(SubTask task, Account account) {
        return false;
    }

    @Override
    public SubTask reportTaskStatus(SubTask wt, SubTaskStatusEnums status, Object result) {
        return wt;
    }

    @Override
    public boolean cronAddTask(Account account) {
//        if (!account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode()) || checkingAccountTCPConnection.containsKey(account.get_id())) {
//            return false;
//        }
//        int wxLastSync = 15 * 1000;
//        Long lastTime = checkingAccountTCPConnectionTime.get(account.get_id());
//        if (lastTime != null && lastTime.compareTo(new Date().getTime() - wxLastSync) > 0) {
//            return false;
//        }
//        checkingAccountTCPConnection.put(account.get_id(), true);
//        // 创建子任务
//        AddTaskDomainDto addTaskDomainDto = new AddTaskDomainDto();
//        addTaskDomainDto.set_id(account.get_id() + "-checkAccountTCPConnection");
//        addTaskDomainDto.setType(CronTaskTypesEnums.CheckAccountTCPConnection.getCode());
//        addTaskDomainDto.setAccid(account.get_id());
//        taskSchedulingService.apiAddTaskByDomain(addTaskDomainDto);
        return false;
    }
}
