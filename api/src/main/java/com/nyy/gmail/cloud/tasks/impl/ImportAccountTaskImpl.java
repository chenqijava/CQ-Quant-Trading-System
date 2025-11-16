package com.nyy.gmail.cloud.tasks.impl;

import com.alibaba.druid.util.StringUtils;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.gateway.GatewayClient;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.service.AccountPlatformService;
import com.nyy.gmail.cloud.tasks.AbstractTask;
import com.nyy.gmail.cloud.tasks.BaseTask;
import com.nyy.gmail.cloud.utils.TaskUtil;
import com.nyy.gmail.cloud.utils.UUIDUtils;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("AccountImport")
public class ImportAccountTaskImpl extends AbstractTask implements BaseTask {

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountPlatformService accountPlatformService;

    @Autowired
    private GatewayClient gatewayClient;

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
        // 导入任务
        String userID = groupTask.getUserID();
        Map params = groupTask.getParams();
        groupTask.setUpdateTime(new Date());

        switch (GroupTaskStatusEnums.fromCode(groupTask.getStatus())) {
            case GroupTaskStatusEnums.waitPublish -> {
                groupTask.setStatus(GroupTaskStatusEnums.processing.getCode());
            }
            case GroupTaskStatusEnums.init, GroupTaskStatusEnums.processing -> {
                // 保存子任务
                int maxInsertCount = 5000;
                long taskCount = subTaskRepository.countByGroupTaskIdEquals(groupTask.get_id());
                List<String> addDatas = taskUtil.getAddData(groupTask.getParams(), maxInsertCount);
                List<SubTask> processingTasks = new ArrayList<>();
                if (!CollectionUtils.isEmpty(addDatas)) {
                    for (String addData : addDatas) {
                        if (StringUtils.isEmpty(addData)) {
                            continue;
                        }
                        String groupId = params.getOrDefault("groupId", "").toString();
                        String openExportReceiveCode = params.getOrDefault("openExportReceiveCode", "").toString();
                        String type = params.getOrDefault("type", "mobile").toString();
                        if (type.equals(AccountTypeEnums.mobile.getCode())) {
                            String[] split = addData.split("----", -1);
                            SubTask subTask = null;
                            if (split.length != 9 && split.length != 10 && split.length != 11 && split.length != 12 && split.length != 13) {
                                Map checkParams = new HashMap(Map.of(
                                        "addData", split[0]
                                ));
                                Map _params = new HashMap(Map.of(
                                        "addData", split[0]
                                ));

                                subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                subTask.setStatus(SubTaskStatusEnums.failed.getCode());
                                subTask.setResult(Map.of("msg", "导入格式不正确"));
                                subTask.setFinishTime(new Date());
                            } else {
                                Map checkParams = new HashMap(Map.of(
                                        "addData", split[0]
                                ));
                                Map _params = new HashMap(Map.of(
                                        "addData", split[0]
                                ));
                                Account account = new Account();
                                String email = split[0];
                                String proxyIp = split[1];
                                String proxyPort = split[2];
                                String proxyUsername = split[3];
                                String proxyPassword = split[4];
                                String app = split[5];
                                String lstBindingKeyAlias = split[6];
                                String googleAccountDataStore = split[7];
                                String device = split[8];
                                String password = "";
                                if (split.length >= 10) {
                                    password = split[9];
                                }
                                String cookie = "";
                                if (split.length >= 11) {
                                    cookie = split[10];
                                }
                                String deviceToken = "";
                                if (split.length >= 13) {
                                    deviceToken = split[12];
                                }
                                if (StringUtil.isNotEmpty(openExportReceiveCode)) {
                                    account.setOpenExportReceiveCode(openExportReceiveCode);
                                }
                                String phone = null;
                                if (split.length >= 12) {
                                    if (!StringUtils.isEmpty(split[11])) {
                                        phone = split[11];
                                    }
                                }
                                account.setPhone(phone);
                                account.setCookie(cookie);
                                account.setPassword(password);
                                account.setProxyUsername(proxyUsername);
                                account.setProxyPort(proxyPort);
                                account.setProxyIp(proxyIp);
                                account.setProxyPassword(proxyPassword);
                                account.setApp(app);
                                account.setDevice(device);
                                account.setLstBindingKeyAlias(lstBindingKeyAlias);
                                account.setGoogleAccountDataStore(googleAccountDataStore);
                                account.setEmail(email);
                                account.setAccID(email);
                                account.setUserID(groupTask.getUserID());
                                account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                                account.setCreateTime(new Date());
                                account.setType(AccountTypeEnums.mobile.getCode());
                                account.setGroupID(groupId);
                                account.setDeviceToken(deviceToken);

                                try {
                                    accountRepository.save(account);
                                    subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                    subTask.setAccid(account.get_id());
                                    subTask.setStatus(SubTaskStatusEnums.success.getCode());
                                    subTask.setFinishTime(new Date());
                                } catch (Exception e) {
                                    subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                    subTask.setStatus(SubTaskStatusEnums.failed.getCode());
                                    subTask.setResult(Map.of("msg", e instanceof DuplicateKeyException ? split[0] + "数据已存在" : e.getMessage()));
                                    subTask.setFinishTime(new Date());
                                }
                            }

                            if (subTask != null) {
                                processingTasks.add(subTask);
                                taskCount++;
                            }
                        } else if (type.equals(AccountTypeEnums.web.getCode())
                                || type.equals(AccountTypeEnums.yahoo.getCode())) {
                            String[] split = addData.split("----", -1);
                            SubTask subTask = null;
                            if (split.length != 1 && split.length != 2) {
                                Map checkParams = new HashMap(Map.of(
//                                        "addData", split[0]
                                ));
                                Map _params = new HashMap(Map.of(
                                        "addData", addData
                                ));

                                subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                subTask.setStatus(SubTaskStatusEnums.failed.getCode());
                                subTask.setResult(Map.of("msg", "导入格式不正确"));
                                subTask.setFinishTime(new Date());
                            } else {
                                Map checkParams = new HashMap(Map.of(
                                        //                                    "addData", split[0]
                                ));
                                Map _params = new HashMap(Map.of(
                                        "addData", addData
                                ));
                                Account account = new Account();
                                String email = "";
                                String cookie = "";
                                if (split.length == 1) {
                                    email = UUIDUtils.get32UUId();
                                    cookie = split[0];
                                } else {
                                    email = split[0];
                                    cookie = split[1];
                                }
                                if (StringUtil.isNotEmpty(openExportReceiveCode)) {
                                    account.setOpenExportReceiveCode(openExportReceiveCode);
                                }
                                account.setPhone("");
                                account.setCookie(cookie);
                                account.setPassword("");
                                account.setProxyUsername("");
                                account.setProxyPort("");
                                account.setProxyIp("");
                                account.setProxyPassword("");
                                account.setApp("");
                                account.setDevice("");
                                account.setLstBindingKeyAlias("");
                                account.setGoogleAccountDataStore("");
                                account.setEmail(email);
                                account.setAccID(email);
                                account.setUserID(groupTask.getUserID());
                                account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                                account.setCreateTime(new Date());
                                account.setType(type);
                                account.setGroupID(groupId);

                                try {
                                    accountRepository.save(account);
                                    subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                    subTask.setAccid(account.get_id());
                                    subTask.setStatus(SubTaskStatusEnums.success.getCode());
                                    subTask.setFinishTime(new Date());
                                } catch (Exception e) {
                                    subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                    subTask.setStatus(SubTaskStatusEnums.failed.getCode());
                                    subTask.setResult(Map.of("msg", e instanceof DuplicateKeyException ? split[0] + "数据已存在" : e.getMessage()));
                                    subTask.setFinishTime(new Date());
                                }
                            }

                            if (subTask != null) {
                                processingTasks.add(subTask);
                                taskCount++;
                            }
                        }  else if (type.equals(AccountTypeEnums.outlook_graph.getCode())) {
                            String[] split = addData.split("----", -1);
                            SubTask subTask = null;
                            if (split.length != 4) {
                                Map checkParams = new HashMap(Map.of(
                                ));
                                Map _params = new HashMap(Map.of(
                                        "addData", addData
                                ));

                                subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                subTask.setStatus(SubTaskStatusEnums.failed.getCode());
                                subTask.setResult(Map.of("msg", "导入格式不正确"));
                                subTask.setFinishTime(new Date());
                            } else {
                                Map checkParams = new HashMap(Map.of(
                                ));
                                Map _params = new HashMap(Map.of(
                                        "addData", addData
                                ));
                                Account account = new Account();
                                String email = split[0];
                                String password = split[1];
                                String outlookGraphClientId = split[2];
                                String outlookGraphRefreshToken = split[3];
                                account.setPhone("");
                                account.setCookie("");
                                account.setPassword(password);
                                account.setProxyUsername("");
                                account.setProxyPort("");
                                account.setProxyIp("");
                                account.setProxyPassword("");
                                account.setApp("");
                                account.setDevice("");
                                account.setLstBindingKeyAlias("");
                                account.setGoogleAccountDataStore("");
                                account.setEmail(email);
                                account.setAccID(email);
                                account.setUserID(groupTask.getUserID());
                                account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                                account.setCreateTime(new Date());
                                account.setType(AccountTypeEnums.outlook_graph.getCode());
                                account.setGroupID(groupId);
                                account.setOutlookGraphClientId(outlookGraphClientId);
                                account.setOutlookGraphRefreshToken(outlookGraphRefreshToken);

                                if (StringUtil.isNotEmpty(openExportReceiveCode)) {
                                    account.setOpenExportReceiveCode(openExportReceiveCode);
                                }

                                try {
                                    accountRepository.save(account);
                                    subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                    subTask.setAccid(account.get_id());
                                    subTask.setStatus(SubTaskStatusEnums.success.getCode());
                                    subTask.setFinishTime(new Date());
                                } catch (Exception e) {
                                    subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                    subTask.setStatus(SubTaskStatusEnums.failed.getCode());
                                    subTask.setResult(Map.of("msg", e instanceof DuplicateKeyException ? split[0] + "数据已存在" : e.getMessage()));
                                    subTask.setFinishTime(new Date());
                                }
                            }

                            if (subTask != null) {
                                processingTasks.add(subTask);
                                taskCount++;
                            }
                        } else if (type.equals(AccountTypeEnums.smtp.getCode())) {
                            String[] split = addData.split("----", -1);
                            SubTask subTask = null;
                            if (split.length != 7 && split.length != 6) {
                                Map checkParams = new HashMap(Map.of(
                                ));
                                Map _params = new HashMap(Map.of(
                                        "addData", addData
                                ));

                                subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                subTask.setStatus(SubTaskStatusEnums.failed.getCode());
                                subTask.setResult(Map.of("msg", "导入格式不正确"));
                                subTask.setFinishTime(new Date());
                            } else {
                                Map checkParams = new HashMap(Map.of(
                                ));
                                Map _params = new HashMap(Map.of(
                                        "addData", addData
                                ));
                                Account account = new Account();
                                String email = split[0];
                                String password = split[1];
                                String smtpHost = split[2];
                                String smtpPort = split[3];
                                String imapHost = split[4];
                                String imapPort = split[5];
                                String ssl = split.length == 6 ? "1" : split[6];
                                account.setPhone("");
                                account.setCookie("");
                                account.setPassword(password);
                                account.setProxyUsername("");
                                account.setProxyPort("");
                                account.setProxyIp("");
                                account.setProxyPassword("");
                                account.setApp("");
                                account.setDevice("");
                                account.setLstBindingKeyAlias("");
                                account.setGoogleAccountDataStore("");
                                account.setEmail(email);
                                account.setAccID(email);
                                account.setUserID(groupTask.getUserID());
                                account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                                account.setCreateTime(new Date());
                                account.setType(AccountTypeEnums.smtp.getCode());
                                account.setGroupID(groupId);
                                account.setSmtpHost(smtpHost);
                                account.setSmtpPort(smtpPort);
                                account.setImapHost(imapHost);
                                account.setImapPort(imapPort);
                                account.setSmtpUsername(email);
                                account.setSmtpPassword(password);
                                account.setImapSsl(ssl.equals("1"));

                                if (StringUtil.isNotEmpty(openExportReceiveCode)) {
                                    account.setOpenExportReceiveCode(openExportReceiveCode);
                                }

                                try {
                                    accountRepository.save(account);
                                    subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                    subTask.setAccid(account.get_id());
                                    subTask.setStatus(SubTaskStatusEnums.success.getCode());
                                    subTask.setFinishTime(new Date());
                                } catch (Exception e) {
                                    subTask = taskUtil.newTaskModel(groupTask, checkParams, _params, null);
                                    subTask.setStatus(SubTaskStatusEnums.failed.getCode());
                                    subTask.setResult(Map.of("msg", e instanceof DuplicateKeyException ? split[0] + "数据已存在" : e.getMessage()));
                                    subTask.setFinishTime(new Date());
                                }
                            }

                            if (subTask != null) {
                                processingTasks.add(subTask);
                                taskCount++;
                            }
                        }
                    }

                    subTaskRepository.batchInsert(processingTasks);
                    groupTaskRepository.save(groupTask);
                    return true;
                } else if (groupTask.getStatus().equals(GroupTaskStatusEnums.processing.getCode())) {
                    groupTask.setTotal(taskCount);
                    groupTask.setPublishTotalCount(taskCount);
                    groupTask.setStatus(GroupTaskStatusEnums.init.getCode());
                    groupTaskRepository.save(groupTask);
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

        accountPlatformService.updateStock(Constants.ADMIN_USER_ID);

        return true;
    }

    @Override
    public boolean checkTask(SubTask task, Account account, Date now) {
        return true;
    }

    @Override
    public boolean runTask(SubTask task, Account account) {
        if (account.getType().equals(AccountTypeEnums.web.getCode())) {
            gatewayClient.makeSession(account);
        }
        return false;
    }
}
