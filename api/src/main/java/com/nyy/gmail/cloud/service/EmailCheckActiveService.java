package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.AccountGroup;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.AccountTypeEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.AccountListDTO;
import com.nyy.gmail.cloud.model.dto.EmailCheckActiveReqDto;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmailCheckActiveService {

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private AccountRepository accountRepository;

    public GroupTask save(EmailCheckActiveReqDto reqDTO, String userID) {
        if (StringUtils.isEmpty(reqDTO.getFilepath()) || StringUtils.isEmpty(reqDTO.getDesc())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        Path resPath = FileUtils.resPath;
        Path path = resPath.resolve(reqDTO.getFilepath()).toAbsolutePath().normalize();
        int i = FileUtils.readCsvFileLineCount(path.toString());

        AccountListDTO accountListDTO = new AccountListDTO();
        accountListDTO.setFilters(new HashMap<>(Map.of( "userID", Constants.ADMIN_USER_ID, "onlineStatus", AccountOnlineStatus.ONLINE.getCode())));
        PageResult<Account> accountPageResult = accountRepository.findByPagination(accountListDTO, Math.min(i, 2000), 1);

        List<String> ids = accountPageResult.getData().stream().map(Account::get_id).collect(Collectors.toList());
        return taskUtil.createGroupTask(ids, TaskTypesEnums.EmailCheckActive, new HashMap(Map.of(
                "taskDesc",reqDTO.getDesc() == null ? "" : reqDTO.getDesc(),
                "publishTotalCount", i,
                "addMethod", "2",
                "filepath", reqDTO.getFilepath() == null ? "" : reqDTO.getFilepath(),
                "reqDto", reqDTO)), userID, "1", new Date());
    }
}
