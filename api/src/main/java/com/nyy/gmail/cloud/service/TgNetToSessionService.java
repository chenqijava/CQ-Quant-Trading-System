package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.BalanceDetailRepository;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import com.nyy.gmail.cloud.utils.ZipUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TgNetToSessionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BalanceDetailRepository balanceDetailRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    public void save(TgNetToSessionReqDto reqDTO, String userID) {
        if (StringUtils.isEmpty(reqDTO.getDesc())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (StringUtils.isEmpty(reqDTO.getFilepath()) || !reqDTO.getFilepath().endsWith(".zip")) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        // 判断文件大小
        File file = new File(FileUtils.resPath.resolve(reqDTO.getFilepath()).toString());

        // 10MB = 10 * 1024 * 1024 bytes
        long fileSizeInBytes = file.length();
        long limit = 10L * 1024 * 1024;

        if (fileSizeInBytes > limit) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "文件不能大于10M");
        }

        String dir = Path.of(file.getParent(), file.getName().replace(".zip", "")).toAbsolutePath().toString();
        try {
            ZipUtil.unzip(file.getAbsolutePath(), dir);
        } catch (Exception e) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        // 计算目录数量
        File currentDir = new File(dir);

        File[] dirs = currentDir.listFiles(File::isDirectory);
        int count = (dirs == null) ? 0 : dirs.length;

        AccountListDTO accountListDTO = new AccountListDTO();
        accountListDTO.setFilters(new HashMap<>());
        accountListDTO.getFilters().put("onlineStatus", AccountOnlineStatus.ONLINE.getCode());
        accountListDTO.getFilters().put("userID", Constants.ADMIN_USER_ID);

        PageResult<Account> pagination = accountRepository.findByPagination(accountListDTO, count, 1);
        if (CollectionUtils.isEmpty(pagination.getData())) {
            throw new CommonException(ResultCode.NO_ONLINE_ACCOUNT);
        }

        User one = userRepository.findOneByUserID(userID);
        if (one == null) {
            throw new CommonException(ResultCode.DATA_NOT_EXISTED);
        }
        if (one.getBalance().compareTo(BigDecimal.valueOf(count)) < 0) {
            throw new CommonException(ResultCode.USER_BALANCE_INSUFFICIENT);
        }
        RLock lock = redissonClient.getLock(userID + "-charge");
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    User user = userRepository.findOneByUserID(userID);
                    if (user.getBalance().compareTo(BigDecimal.valueOf(count)) < 0) {
                        throw new CommonException(ResultCode.USER_BALANCE_INSUFFICIENT);
                    }
                    user.setBalance(user.getBalance().subtract(BigDecimal.valueOf(count)));
                    userRepository.updateUser(user);
                    balanceDetailRepository.addUserBill("任务扣款",
                            user.getUserID(), BigDecimal.valueOf(count), user.getBalance(), user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.BUY_ORDER);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
        }

        List<String> ids = pagination.getData().stream().map(Account::get_id).collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("tgNetDir", dir);
        params.put("taskDesc", reqDTO.getDesc());
        params.put("filepath", reqDTO.getFilepath());
        params.put("publishTotalCount", count);

        taskUtil.createGroupTask(ids, TaskTypesEnums.TgNetToSession, params, Session.currentSession().userID);
    }

    public PageResult<GroupTask> findByPagination(Params params, Integer pageSize, Integer pageNum) {
        return groupTaskRepository.findByPagination(pageSize, pageNum, params.getFilters());
    }

    public void delete(IdsListDTO data) {
        for (String id : data.getIds()) {
            GroupTask groupTask = groupTaskRepository.findById(id).orElse(null);
            if (groupTask != null && groupTask.getUserID().equals(Session.currentSession().getUserID()) && (groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode()) || groupTask.getStatus().equals(GroupTaskStatusEnums.failed.getCode()))) {
                groupTaskRepository.delete(groupTask);
            }
        }
    }

    public void stop(IdsListDTO data) {
        for (String id : data.getIds()) {
            GroupTask groupTask = groupTaskRepository.findById(id).orElse(null);
            if (groupTask != null && groupTask.getUserID().equals(Session.currentSession().getUserID()) && (!groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode()) && !groupTask.getStatus().equals(GroupTaskStatusEnums.failed.getCode()))) {
                groupTask.setStatus(GroupTaskStatusEnums.init.getCode());
                groupTaskRepository.save(groupTask);
                taskUtil.finishForceTaskByPublish(groupTask);
            }
        }
    }
}
