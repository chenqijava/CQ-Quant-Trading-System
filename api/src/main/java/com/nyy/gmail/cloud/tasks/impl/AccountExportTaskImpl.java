package com.nyy.gmail.cloud.tasks.impl;

import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.model.dto.AccountListDTO;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.service.AccountPlatformService;
import com.nyy.gmail.cloud.service.BuyEmailOrderService;
import com.nyy.gmail.cloud.service.ParamsService;
import com.nyy.gmail.cloud.tasks.AbstractTask;
import com.nyy.gmail.cloud.tasks.BaseTask;
import com.nyy.gmail.cloud.utils.DateUtil;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import com.nyy.gmail.cloud.utils.UUIDUtils;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component("AccountExport")
public class AccountExportTaskImpl extends AbstractTask implements BaseTask {

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ParamsService paramsService;

    @Autowired
    private AccountExportRecordRepository accountExportRecordRepository;

    @Autowired
    private AccountPlatformService accountPlatformService;

    @Autowired
    private BuyEmailOrderDetailRepository buyEmailOrderDetailRepository;

    @Autowired
    private BuyEmailOrderRepository  buyEmailOrderRepository;

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
        String userID = groupTask.getUserID();
        Map params = groupTask.getParams();
        groupTask.setUpdateTime(new Date());
        String platformId = params.get("platformId").toString();
        Integer count = Integer.valueOf(params.get("count").toString());
        String recordId = params.get("recordId").toString();
        List<String> ids = (List<String>) params.get("ids");
        String orderId = params.get("orderId").toString();

        RLock lock = redissonClient.getLock("AccountExport:" + platformId);
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    switch (GroupTaskStatusEnums.fromCode(groupTask.getStatus())) {
                        case GroupTaskStatusEnums.waitPublish -> {
                            groupTask.setStatus(GroupTaskStatusEnums.processing.getCode());
                        }
                        case GroupTaskStatusEnums.init, GroupTaskStatusEnums.processing -> {
                            // 保存子任务
                            List<Account> accountList = null;
                            if (ids.isEmpty()) {
                                AccountListDTO accountListDTO = getAccountListDTO(platformId, userID);
                                accountList = accountRepository.findByPagination(accountListDTO, count + 100, 1).getData();
                            } else {
                                count = ids.size();
                                AccountListDTO accountListDTO = getAccountListDTO(platformId, userID, ids);
                                accountList = accountRepository.findByPagination(accountListDTO, ids.size(), 1).getData();
                            }
                            // 打乱顺序
                            Collections.shuffle(accountList);
                            int used = 0;
                            for (Account account : accountList) {
                                if (used >= count) {
                                    break;
                                }
                                for (int i = 0; i < 3; i++) {
                                    try {
                                        account = accountRepository.findById(account.get_id());
                                        if (account.getUsedPlatformIds() == null) {
                                            account.setUsedPlatformIds(new ArrayList<>());
                                        }
                                        // 确保没有使用
                                        if (account.getUsedPlatformIds().contains(platformId)) {
                                            break;
                                        }
                                        account.getUsedPlatformIds().add(platformId);
                                        account.setUsed(account.getUsed() == null ? 1 : (account.getUsed() + 1));
                                        accountRepository.update(account);

                                        SubTask subTask = taskUtil.newTaskModel(groupTask, Map.of(), Map.of("email", account.getEmail(), "platformId", platformId), null);
                                        subTask.setAccid(account.get_id());
                                        subTask.setStatus(SubTaskStatusEnums.success.getCode());
                                        subTask.setFinishTime(new Date());
                                        subTaskRepository.save(subTask);

                                        if (StringUtils.isNotEmpty(orderId)) {
                                            BuyEmailOrder order = buyEmailOrderRepository.findById(orderId);
                                            if (order != null && order.getStatus().equals(BuyEmailOrderStatus.doing.getCode())) {
                                                String emailExpireTime = paramsService.getParams("account.emailExpireTime", null, null).toString();

                                                BuyEmailOrderDetail detail = new BuyEmailOrderDetail();
                                                detail.setEmail(account.getEmail());
                                                detail.setOrderDetailNo(UUIDUtils.getBuyEMailOrderDetailNo(order.getOrderNo(), used));
                                                detail.setOrderId(orderId);
                                                detail.setPrice(order.getUnitPrice());
                                                detail.setText("");
                                                detail.setStatus(BuyEmailDetailOrderStatus.Waiting.getCode());
                                                detail.setCreateTime(new Date());
                                                detail.setUpdateTime(new Date());
                                                Calendar instance = Calendar.getInstance();
                                                instance.add(Calendar.MINUTE, Integer.parseInt(emailExpireTime));
                                                detail.setExpireTime(instance.getTime());
                                                detail.setPlatformId(order.getPlatformId());
                                                detail.setPlatformName(order.getPlatformName());
                                                detail.setUserID(order.getUserID());
                                                detail.setSubTaskId(subTask.get_id());
                                                detail.setAccid(account.get_id());
                                                buyEmailOrderDetailRepository.save(detail);
                                            }
                                        }
                                        used++;
                                        break;
                                    } catch (Exception e) {
                                        log.error("导出账号报错，" + e.getMessage());
                                    }
                                }
                            }

                            if (accountList.isEmpty()) {
                                if (StringUtils.isNotEmpty(orderId)) {
                                    BuyEmailOrder order = buyEmailOrderRepository.findById(orderId);
                                    if (order != null && order.getStatus().equals(BuyEmailOrderStatus.doing.getCode())) {
                                        String emailExpireTime = paramsService.getParams("account.emailExpireTime", null, null).toString();

                                        for (int i = 0; i < order.getBuyNum(); i++) {
                                            BuyEmailOrderDetail detail = new BuyEmailOrderDetail();
                                            detail.setEmail(BuyEmailOrderService.outOfStockEmail);
                                            detail.setOrderDetailNo(UUIDUtils.getBuyEMailOrderDetailNo(order.getOrderNo(), i));
                                            detail.setOrderId(orderId);
                                            detail.setPrice(order.getUnitPrice());
                                            detail.setText("");
                                            detail.setStatus(BuyEmailDetailOrderStatus.Waiting.getCode());
                                            detail.setCreateTime(new Date());
                                            detail.setUpdateTime(new Date());
                                            Calendar instance = Calendar.getInstance();
                                            instance.add(Calendar.MINUTE, Integer.parseInt(emailExpireTime));
                                            detail.setExpireTime(instance.getTime());
                                            detail.setPlatformId(order.getPlatformId());
                                            detail.setPlatformName(order.getPlatformName());
                                            detail.setUserID(order.getUserID());
                                            detail.setSubTaskId("");
                                            detail.setAccid("");
                                            buyEmailOrderDetailRepository.save(detail);
                                        }
                                    }
                                }
                            }
                        }
                        case null -> {
                        }
                        default -> {
                        }
                    }

                    switch (GroupTaskUserActionEnums.fromCode(groupTask.getUserAction())) {
                        case null -> {
                        }
                        case ForceFinish -> {
                            taskUtil.finishForceTaskByPublish(groupTask);
                        }
                    }
                    if (StringUtil.isNotEmpty(groupTask.getUserAction())) {
                        groupTask.setUserAction("");
                        groupTaskRepository.save(groupTask);
                        return true;
                    }

                    long successCount = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.success.getCode());
                    long failedCount = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.failed.getCode());
                    groupTask.setFailed(failedCount);
                    groupTask.setSuccess(successCount);
                    groupTask.setPublishStatus("success");
                    groupTask.setFinishTime(new Date());
                    groupTask.setStatus(GroupTaskStatusEnums.success.getCode());
                    groupTask.setTotal(failedCount + successCount);
                    groupTask.setPublishedCount(groupTask.getTotal());
                    groupTaskRepository.save(groupTask);

                    String baseUrl = paramsService.getParams("webConfig.baseUrl", null, null).toString();
                    List<SubTask> subTasks = subTaskRepository.findByGroupTaskId(groupTask.get_id());
                    List<String> lines = subTasks.stream().map(e -> {
                            if (groupTask.getParams().containsKey("exportType")) {
                                String exportType = groupTask.getParams().get("exportType").toString();
                                String line = e.getParams().get("email").toString();
                                Account account = accountRepository.findById(e.getAccid());
                                if (exportType.contains("origin")) {
                                    line = line + "----" + baseUrl + "/api/latest/code?id=" + e.get_id()+"&aid=" + e.getAccid();
                                }
                                if (exportType.contains("password")) {
                                    line = line + "----" + (account != null && account.getPassword() != null ? account.getPassword() : "");
                                }
                                if (exportType.contains("cookie")) {
                                    line = line + "----" + (account != null && account.getCookie() != null ? account.getCookie() : "");
                                }
                                return line;
                            } else {
                                return e.getParams().get("email") + "----" + baseUrl + "/api/latest/code?id=" + e.get_id()+"&aid=" + e.getAccid();
                            }
                    }).toList();
                    // 导出文件保存
                    Path resPath = FileUtils.resPath;
                    Path filepath = Path.of("exportAccount", "gmail" + UUIDUtils.get32UUId().substring(26) + "_" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYYMMDDHHMMSS) + "_" + lines.size() + ".txt");
                    Path path = resPath.resolve(filepath).toAbsolutePath().normalize();

                    try {
                        Files.createDirectories(resPath.resolve("exportAccount").normalize());
                    } catch (IOException e) {
                        log.error("创建upload文件夹失败", e);
                    }
                    if (Files.exists(path)) {
                        Files.delete(path);
                    }
                    Files.writeString(path, String.join("\n", lines), StandardOpenOption.CREATE_NEW);

                    AccountExportRecord record = accountExportRecordRepository.findById(recordId);
                    if (record != null) {
                        record.setFilepath(filepath.toString());
                        record.setNumber(subTasks.size());
                        accountExportRecordRepository.update(record);
                    }
                } catch (IOException e) {
                    log.error("导出文件失败", e);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
        }

        accountPlatformService.updateStock(Constants.ADMIN_USER_ID);

        return true;
    }

    @NotNull
    private static AccountListDTO getAccountListDTO(String platformId, String userID) {
        AccountListDTO accountListDTO = new AccountListDTO();
        accountListDTO.setSorter(Map.of("lastPullMessageTime", -1));
//        Calendar instance = Calendar.getInstance();
//        instance.add(Calendar.HOUR, -3);
        if (userID.equals(Constants.ADMIN_USER_ID)) {
            accountListDTO.setFilters(Map.of(
                    "usedPlatformIds", Map.of("$ne", platformId),
                    "onlineStatus", "1",
                    "isCheck", true,
//                "createTime", Map.of("$gt", instance.getTime()),
//                    "userID", userID,
                    "openExportReceiveCode", "1",
                    "type", Map.of("$nin", List.of(AccountTypeEnums.sendgrid.getCode()))
            ));
        } else {
            accountListDTO.setFilters(Map.of(
                    "usedPlatformIds", Map.of("$ne", platformId),
                    "onlineStatus", "1",
                    "isCheck", true,
//                "createTime", Map.of("$gt", instance.getTime()),
                    "userID", userID,
                    "openExportReceiveCode", "1",
                    "type", Map.of("$nin", List.of(AccountTypeEnums.sendgrid.getCode()))
            ));
        }

        return accountListDTO;
    }

    @NotNull
    private static AccountListDTO getAccountListDTO(String platformId, String userID, List<String> ids) {
        AccountListDTO accountListDTO = new AccountListDTO();
        accountListDTO.setSorter(Map.of("lastPullMessageTime", 1));
//        Calendar instance = Calendar.getInstance();
//        instance.add(Calendar.HOUR, -3);
        if (userID.equals(Constants.ADMIN_USER_ID)) {
            accountListDTO.setFilters(Map.of(
                            "usedPlatformIds", Map.of("$ne", platformId),
                            "onlineStatus", "1",
                            "isCheck", true,
                            //                "createTime", Map.of("$gt", instance.getTime()),
                            "_id", Map.of("$in", ids),
                            "openExportReceiveCode", "1",
                            "type", Map.of("$nin", List.of(AccountTypeEnums.sendgrid.getCode()))
                    )
            );
        } else {
            accountListDTO.setFilters(Map.of(
                            "usedPlatformIds", Map.of("$ne", platformId),
                            "onlineStatus", "1",
                            "isCheck", true,
                            //                "createTime", Map.of("$gt", instance.getTime()),
                            "_id", Map.of("$in", ids),
                            "openExportReceiveCode", "1",
                            "type", Map.of("$nin", List.of(AccountTypeEnums.sendgrid.getCode())),
                            "userID", userID
                    )
            );
        }
        return accountListDTO;
    }

    @Override
    public boolean checkTask(SubTask task, Account account, Date now) {
        return true;
    }

    @Override
    public boolean runTask(SubTask task, Account account) {
        return true;
    }
}
