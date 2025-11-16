package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GoogleStudioApiKey;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.model.dto.AccountListDTO;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.service.Socks5Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class ReleaseSocks5Job {

    private volatile boolean is_running = false;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Socks5Service socks5Service;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Value("${application.taskType}")
    private String taskType;

    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;

    @Autowired
    private GoogleStudioApiKeyRepository googleStudioApiKeyRepository;

    @Autowired
    private Socks5Repository socks5Repository;

    @Async("async")
    @Scheduled(cron = "0 * * * * *")
    public void run() {
//        if (taskType.equals("googleai")) {
//            return;
//        }

        if (is_running) {
            return;
        }
        is_running = true;
        try {
            log.info("开始ReleaseSocks5Job");
//            List<User> all = userRepository.findAll();
//            for (User user : all) {
//                if (user.getStatus().equals("enable")) {
//                    // 分页优化
//                    int page = 1;
//                    while (true) {
//                        AccountListDTO accountListDTO = new AccountListDTO();
//                        accountListDTO.setFilters(new HashMap<>(Map.of("onlineStatus", AccountOnlineStatus.OFFLINE.getCode(), "userID", user.getUserID())));
//                        log.info("开始ReleaseSocks5Job page:" + page +" userID:" + user.getUserID());
//                        PageResult<Account> pagination = accountRepository.findByPagination(accountListDTO, 100, page);
//                        if (pagination.getData().isEmpty()) {
//                            break;
//                        }
//                        for (Account account : pagination.getData()) {
//                            if (StringUtils.isNotEmpty(account.getSocks5Id())) {
//                                try {
//                                    socks5Service.releaseSocks5(account);
//                                } catch (Exception e) {
//                                    log.info("ReleaseSocks5Job ERROR:" +e.getMessage());
//                                }
//                            }
//                        }
//                        page++;
//                    }
//                }
//            }

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -5);
            int page = 1;
            while (true) {
                PageResult<Socks5> pageResult = mongoPaginationHelper
                        .query(MongoPaginationBuilder.builder(Socks5.class)
                                .filters(Map.of("proxyAccount", Map.of("$exists", true), "createTime", Map.of("$lt", calendar.getTime())))
                                .enableLog(false).pageSize(100).page(page).build());
                if (pageResult.getData().isEmpty()) {
                    break;
                }
                List<String> ids = pageResult.getData().stream().map(Socks5::get_id).toList();
                List<Account> accounts = accountRepository.findBySocks5IdListAndStatus(ids, AccountOnlineStatus.ONLINE.getCode());
                List<GoogleStudioApiKey> googleStudioApiKeyList = googleStudioApiKeyRepository.findBySocks5IdListAndStatus(ids, "enable");
                Set<String> keepIds =  new HashSet<>();
                for (GoogleStudioApiKey googleStudioApiKey : googleStudioApiKeyList) {
                    keepIds.add(googleStudioApiKey.getSocks5Id());
                }
                for (Account account : accounts) {
                    keepIds.add(account.getSocks5Id());
                }
//                for (Socks5 socks5 : pageResult.getData()) {
//                    if (!keepIds.contains(socks5.get_id())) {
//                        if (StringUtils.isNotEmpty(socks5.getProxyAccount())) {
//                            socks5Repository.deleteSocks5ById(socks5.get_id());
//                        }
//                    }
//                }
                List<String> removeIds = pageResult.getData().stream()
                        .filter(socks5 -> StringUtils.isNotEmpty(socks5.getProxyAccount()) && !keepIds.contains(socks5.get_id()))
                        .map(Socks5::get_id)
                        .toList();

                if (!removeIds.isEmpty()) {
                    socks5Repository.deleteSocks5ByIds(removeIds);
                }

                page++;
            }
        } finally {
            log.info("结束ReleaseSocks5Job");
            is_running = false;
        }
    }


    private volatile boolean is_running2 = false;


    @Async("async")
    @Scheduled(cron = "0 0/5 * * * ?")
    public void checkSendEmailNum() {
        if (is_running2) {
            return;
        }
        is_running2 = true;
        try {
            log.info("开始checkSendEmailNum");
            List<User> all = userRepository.findAll();
            for (User user : all) {
                if (user.getStatus().equals("enable")) {
                    // 分页优化
                    int page = 1;
                    while (true) {
                        AccountListDTO accountListDTO = new AccountListDTO();
                        accountListDTO.setFilters(new HashMap<>(Map.of("sendEmailNumByDay", Map.of("$gt", 0), "userID", user.getUserID())));
                        PageResult<Account> pagination = accountRepository.findByPagination(accountListDTO, 100, page);
                        if (pagination.getData().isEmpty()) {
                            break;
                        }
                        for (Account account : pagination.getData()) {
                            long l = subTaskRepository.countSendEmailOneDayByAccid(account.get_id());
                            long l1 = subTaskRepository.countSendEmailOneDayByAccidSuccess(account.get_id());
                            accountRepository.updateSendEmailNum(account.get_id(), l, l1);
                        }
                        page++;
                    }

                    page = 1;
                    while (true) {
                        AccountListDTO accountListDTO = new AccountListDTO();
                        accountListDTO.setFilters(new HashMap<>(Map.of("onlineStatus", AccountOnlineStatus.ONLINE.getCode(), "userID", user.getUserID())));
                        PageResult<Account> pagination = accountRepository.findByPagination(accountListDTO, 100, page);
                        if (pagination.getData().isEmpty()) {
                            break;
                        }
                        for (Account account : pagination.getData()) {
                            long l = subTaskRepository.countSendEmailOneDayByAccid(account.get_id());
                            long l1 = subTaskRepository.countSendEmailOneDayByAccidSuccess(account.get_id());
                            accountRepository.updateSendEmailNum(account.get_id(), l, l1);
                        }
                        page++;
                    }
                }
            }
        } finally {
            log.info("结束checkSendEmailNum");
            is_running2 = false;
        }

    }
}
