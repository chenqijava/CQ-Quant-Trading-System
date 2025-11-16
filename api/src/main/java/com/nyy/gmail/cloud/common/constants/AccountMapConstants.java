package com.nyy.gmail.cloud.common.constants;

import com.nyy.gmail.cloud.common.enums.AccountRunTaskStatusEnum;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class AccountMapConstants {

    public static final Map<String, AccountCacheInfo> ACCOUNT_MAP = new HashMap<>();

    @Data
    class AccountCacheInfo {

        private String accid;

        private List<SubTask> subTasks;

        private AccountRunTaskStatusEnum status;
    }
}
