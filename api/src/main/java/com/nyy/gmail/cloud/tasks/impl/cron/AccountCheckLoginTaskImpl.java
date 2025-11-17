package com.nyy.gmail.cloud.tasks.impl.cron;

import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.AccountOtherStatusTypeEnums;
import com.nyy.gmail.cloud.enums.CronTaskTypesEnums;
import com.nyy.gmail.cloud.enums.SubTaskStatusEnums;
import com.nyy.gmail.cloud.framework.TaskSchedulingService;
import com.nyy.gmail.cloud.framework.dto.AddTaskDomainDto;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.service.Socks5Service;
import com.nyy.gmail.cloud.tasks.CronBaseTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component("AccountCheckLogin")
public class AccountCheckLoginTaskImpl implements CronBaseTask {

    @Autowired
    private TaskSchedulingService taskSchedulingService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private Socks5Service socks5Service;

    private static final Map<String, Long> checkingAccountLoginTime = new HashMap<>();

    private static final Map<String, Boolean> checkingAccountLogin = new HashMap<>();

    @Override
    public boolean cronAddTask(Account account) {
//        accountRepository.loginTimeoutChangeOnlineStatus();
//        // 已经登录过的不在检查session
//        if (StringUtils.isNotEmpty(account.getLoginSession())) {
//            return  false;
//        }
//        if (!account.getOnlineStatus().equals(AccountOnlineStatus.WAITING_ONLINE.getCode()) || checkingAccountLogin.containsKey(account.get_id())) {
//            return false;
//        }
//        int wxLastSync = 3 * 1000;
//        Long lastTime = checkingAccountLoginTime.get(account.get_id());
//        if (lastTime != null && lastTime.compareTo(new Date().getTime() - wxLastSync) > 0) {
//            return false;
//        }
//
//        checkingAccountLogin.put(account.get_id(), true);
//
//        // 创建子任务
//        AddTaskDomainDto addTaskDomainDto = new AddTaskDomainDto();
//        addTaskDomainDto.set_id(account.get_id() + "-accountCheckLogin");
//        addTaskDomainDto.setType(CronTaskTypesEnums.AccountCheckLogin.getCode());
//        addTaskDomainDto.setAccid(account.get_id());
//        taskSchedulingService.apiAddTaskByDomain(addTaskDomainDto);
        return true;
    }

    @Override
    public boolean checkTask(SubTask task, Account account, Date now) {
        return true;
    }

    @Override
    public boolean runTask(SubTask task, Account account) {
//        try {
//            if (account == null || StringUtils.isEmpty(account.getSession())) {
//                return false;
//            }
//            try {
//                GatewayResult<GetSessionResponse> session = gatewayClient.getSession(account);
//                if (session != null && session.getData() != null && StringUtils.isNotEmpty(session.getData().getSession())) {
//                    GatewayResult<LoginResponse> login = gatewayClient.login(account, session.getData().getSession());
//                    if (!login.getCode().equals(0)) {
//                        accountRepository.updateOnlineStatusOtherStatus(account.get_id(), AccountOnlineStatus.OFFLINE.getCode(), login.getMsg(), AccountOtherStatusTypeEnums.LOGIN_FAIL);
//                        socks5Service.releaseSocks5(account.get_id(), account.getSocks5Id(), account.getVpsID());
//                    }
//                }
//            } catch (Exception e) {
//                log.error("{} AccountCheckLoginJob One Task run error: {}", account.get_id(), e.getMessage());
//            } finally {
//                log.info("{} AccountCheckLoginJob One Task run finish", account.get_id());
//            }
//        } finally {
//            if (account != null) {
//                checkingAccountLogin.remove(account.get_id());
//                checkingAccountLoginTime.put(account.get_id(), new Date().getTime());
//            }
//        }
        return true;
    }

    @Override
    public SubTask reportTaskStatus(SubTask wt, SubTaskStatusEnums status, Object result) {
        return wt;
    }
}
