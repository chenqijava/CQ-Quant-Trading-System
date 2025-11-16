package com.nyy.gmail.cloud.tasks.impl;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.framework.mq.SubTaskMQProducer;
import com.nyy.gmail.cloud.mq.entity.TaskMessage;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.service.ParamsService;
import com.nyy.gmail.cloud.service.ProxyAccountService;
import com.nyy.gmail.cloud.service.Socks5Service;
import com.nyy.gmail.cloud.service.ai.AiImageRecognition;
import com.nyy.gmail.cloud.tasks.AbstractTask;
import com.nyy.gmail.cloud.tasks.BaseTask;
import com.nyy.gmail.cloud.utils.GoogleGenAiUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component("ImageRecognition")
public class ImageRecognitionTaskImpl extends AbstractTask implements BaseTask {
    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SubTaskMQProducer subTaskMQProducer;

    @Autowired
    private GoogleStudioApiKeyRepository googleStudioApiKeyRepository;

    @Autowired
    private ProxyAccountService proxyAccountService;

    @Autowired
    private Socks5Repository socks5Repository;

    @Autowired
    private Socks5Service socks5Service;

    @Autowired
    private ParamsService paramsService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    protected SubTaskRepository getSubTaskRepository() {
        return subTaskRepository;
    }

    @Override
    protected TaskUtil getTaskUtil() {
        return taskUtil;
    }

    private volatile Date prevNextUseTime = null;

    @Override
    public boolean publishTask(GroupTask groupTask) {
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
                    int onceImageNum = Integer.parseInt(paramsService.getParams("account.onceImageNum", null, null).toString());
                    // AAA 调整可以控制 并发数量 单个任务发布10个图片
                    for (int i = 0; i < addDatas.size(); i += onceImageNum) {
                        List<String> urls = new ArrayList<>();
                        for (int j = 0;j < onceImageNum && (i + j) < addDatas.size(); j++) {
                            String addData = addDatas.get(i + j);
                            if (StringUtils.isEmpty(addData)) {
                                continue;
                            }
                            urls.add(addData);
                        }
                        if (!urls.isEmpty()) {
                            SubTask subTask = taskUtil.newTaskModel(groupTask, Map.of(), Map.of("imageUrls", urls, "promptVersion", params.getOrDefault("promptVersion", "v1")), new Date());
                            processingTasks.add(subTask);
                            taskCount++;
                        }
                    }

                    subTaskRepository.batchInsert(processingTasks);
                    groupTaskRepository.save(groupTask);
                    return true;
                } else if (groupTask.getStatus().equals(GroupTaskStatusEnums.processing.getCode())) {
                    groupTask.setTotal(taskCount);
                    groupTask.setPublishTotalCount(taskCount);
                    groupTask.setPublishedCount(taskCount);
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
        int onceMaxCount = 2001;

        List<SubTask> subTaskList = null;
        List<String> ids = groupTask.getIds();
        Map<String, Account> accountMap = new HashMap<>();
        List<TaskMessage> mqMsg = new ArrayList<>();
        int count = 0;
        Calendar instance = Calendar.getInstance();
        // 限制并发 没执行成功 30s内不能使用
        instance.add(Calendar.MINUTE, 10);

        List<GoogleStudioApiKey> googleStudioApiKeyList = googleStudioApiKeyRepository.findByCanUsed(onceMaxCount, groupTask.getUserID(), null, AiTypeEnums.GoogleStudio)
                            .stream().sorted(Comparator.comparing(GoogleStudioApiKey::getUseByDay)).filter(e -> e.getUseByDay() < e.getLimitByDay() && e.getUseByMinute() < e.getLimitByMinute()).collect(Collectors.toList());

        if (googleStudioApiKeyList.isEmpty()) {
            return false;
        }

        if (googleStudioApiKeyList.size() > 10) {
            googleStudioApiKeyList = googleStudioApiKeyList.subList(0, googleStudioApiKeyList.size() * 3 / 4);
            Collections.shuffle(googleStudioApiKeyList);
        }

        subTaskList = subTaskRepository.findByUserIDEqualsAndGroupTaskIdEqualsAndStatusIn(userID, groupTask.get_id(), List.of(SubTaskStatusEnums.processing.getCode()), 1, googleStudioApiKeyList.size());

        log.info("{} googleStudioApiKeyList:{} subTaskList: {} ids: {}", groupTask.get_id(), googleStudioApiKeyList.size(), subTaskList.size(), ids.size());

        if (googleStudioApiKeyList != null && subTaskList != null) {
            // 排除正在使用中的
            List<SubTask> subTasks = subTaskRepository.findByStatus(SubTaskStatusEnums.init.getCode());
            Set<String> useIds = subTasks.stream().map(SubTask::getAccid).collect(Collectors.toSet());
            ids = ids.stream().filter(e -> !useIds.contains(e)).toList();
            boolean isLog = Math.random() >= 0.9;
            if (isLog) {
                log.info("SubTask distributeAccount 0 {} ", groupTask.get_id());
            }

            for (SubTask subTask : subTaskList) {
                isLog = Math.random() >= 0.9;
                // 随机
                String accid = ids.get(count % ids.size());
                GoogleStudioApiKey googleStudioApiKey = googleStudioApiKeyList.get((count) % googleStudioApiKeyList.size());

                Account account = accountMap.get(accid);
                if (account == null) {
                    account = accountRepository.findById(accid);
                }
                if (account == null) {
                    continue;
                } else {
                    accountMap.put(accid, account);
                }
                if (isLog) {
                    log.info("SubTask distributeAccount 1 {} ", subTask.get_id());
                }
                if (account.getUserID().equals(groupTask.getUserID())) {
                    if (subTask.getParams() == null) {
                        subTask.setParams(new HashMap());
                    }
                    subTask.getParams().put("apiKeyId", googleStudioApiKey.get_id());
                    taskUtil.distributeAccount(account, subTask);

                    if (isLog) {
                        log.info("SubTask distributeAccount 2 {} ", subTask.get_id());
                    }
                    try {
                        // 提前分配socks
                        if (StringUtils.isEmpty(googleStudioApiKey.getSocks5Id())) {
                            Socks5 socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                            if (socks5 != null) {
                                googleStudioApiKey.setSocks5Id(socks5.get_id());
                            }
                        }
                        // 优化
                        String socks5Id = googleStudioApiKey.getSocks5Id();
                        googleStudioApiKey.setNextUseTime(instance.getTime());
                        googleStudioApiKey.setSocks5Id(socks5Id);
                        googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), googleStudioApiKey.getNextUseTime(), null);
                        mqMsg.add(taskUtil.domain2mqTask(subTask));
                    } catch (Exception e) {
                        log.error("update googleStudioApiKey error: {}", e.getMessage());
                    }
                    if (isLog) {
                        log.info("SubTask distributeAccount 3 {} ", subTask.get_id());
                    }
                }

                count++;
            }

            if (!mqMsg.isEmpty()) {
                subTaskMQProducer.sendMessage(mqMsg);
            }
        }

        try {
            if (subTaskList != null) {
                if (subTaskList.isEmpty() && groupTask.getSuccess() + groupTask.getFailed() >= groupTask.getTotal()) {
                    long success = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.success.getCode());
                    long failed = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.failed.getCode());
                    if (groupTask.getSuccess() != success) {
                        groupTask.setSuccess(success);
                    }
                    if (groupTask.getFailed() != failed) {
                        groupTask.setFailed(failed);
                    }
                    if (groupTask.getSuccess() + groupTask.getFailed() >= groupTask.getTotal()) {
                        groupTask.setPublishStatus("success");
                        groupTask.setFinishTime(new Date());
                        if (!groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode())) {
                            groupTask.setStatus(GroupTaskStatusEnums.success.getCode());
                        }
                    } else {
                        groupTask.setStatus(GroupTaskStatusEnums.init.getCode());
                    }
                    groupTaskRepository.save(groupTask);
                } else {
                    if (subTaskList.isEmpty()) {
                        long success = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.success.getCode());
                        long failed = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.failed.getCode());
                        if (groupTask.getSuccess() != success) {
                            groupTask.setSuccess(success);
                        }
                        if (groupTask.getFailed() != failed) {
                            groupTask.setFailed(failed);
                        }

                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.MINUTE, -2);
                        List<SubTask> restart = subTaskRepository.findRestart(groupTask.get_id(), calendar.getTime());
                        for (SubTask subTask : restart) {
                            try {
                                subTask = subTaskRepository.findById(subTask.get_id());
                                if (subTask.getStatus().equals(SubTaskStatusEnums.init.getCode()) && subTask.getUpdateTime().before(calendar.getTime())) {
                                    subTask.setUpdateTime(new Date());
                                    subTask.setStatus(SubTaskStatusEnums.processing.getCode());
                                    subTask.setAccid("");
                                    subTask.getParams().put("apiKeyId", "");
                                    subTaskRepository.save(subTask);
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                    groupTaskRepository.save(groupTask);
                }
            }
        } catch (OptimisticLockingFailureException e) {
            log.info("{} OptimisticLockingFailureException: {}", groupTask.get_id(), e.getMessage());
        }
        return true;
    }

    @Override
    public boolean checkTask(SubTask task, Account account, Date now) {

        return true;
    }

    @Override
    public boolean runTask(SubTask task, Account account) {
        String apiKeyId = task.getParams().get("apiKeyId").toString();
        boolean flag = false;
        int waitInterval = 5;
        SubTask wt = null;
        try {
            wt = subTaskRepository.findById(task.get_id());
            if (wt == null) {
                log.info("id {} 任务不存在", task.get_id());
                return false;
            }
            try {
                if (!wt.getStatus().equals(SubTaskStatusEnums.init.getCode())) {
                    log.info("id {} 任务状态不正确", task.get_id());
                    return false;
                }
                GroupTask groupTask = groupTaskRepository.findById(task.getGroupTaskId()).orElse(null);
                if (groupTask == null) {
                    this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "任务已经结束");
                    return false;
                }
                if (groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode())) {
                    this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "任务已经结束");
                    return false;
                }
                if (!account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode())) {
                    this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "账号不在线");
                    return false;
                }
                List<String> imageUrls = (List<String>) task.getParams().get("imageUrls");
                String promptVersion = task.getParams().getOrDefault("promptVersion", "v1").toString();
                int retry = MapUtils.getInteger(task.getParams(), "retry", 0);
                if (retry >= 3) {
                    this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "尝试识别3次不成功");
                    return false;
                }
                // 防止太快
                for (int i = 0; i < 30; i++) {
                    RLock lock = redissonClient.getLock("ImageRecognitionLock:" + apiKeyId);
                    if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                        break;
                    } else {
                        Thread.sleep(1000);
                    }
                    if (i == 29) {
                        apiKeyId = "";
                        wt = subTaskRepository.findById(task.get_id());
                        wt.setStatus(SubTaskStatusEnums.processing.getCode());
                        wt.setAccid("");
                        wt.getParams().put("apiKeyId", "");
                        subTaskRepository.save(wt);
                        return false;
                    }
                }


                List<List<GoogleGenAiUtils.Result>> results = null;
                for (int i = 0; i < 5; i++) {
                    try {
                        GoogleStudioApiKey googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(apiKeyId, task.getUserID());
                        if (googleStudioApiKey == null) {
                            break;
                        }
                        if (i > 0) {
                            socks5Service.releaseSocks5(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null);
                            googleStudioApiKey.setSocks5Id("");
                        }
                        Socks5 socks5 = null;
                        if (StringUtils.isEmpty(googleStudioApiKey.getSocks5Id())) {
                            socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                            if (socks5 == null) {
                                throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                            }
                            googleStudioApiKey.setSocks5Id(socks5.get_id());
                            googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
                        } else {
                            socks5 = socks5Repository.findSocks5ById(googleStudioApiKey.getSocks5Id());
                            if (socks5 == null) {
                                socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                                if (socks5 == null) {
                                    throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                                }
                                googleStudioApiKey.setSocks5Id(socks5.get_id());
                                googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
                            }
                        }
                        if (socks5 == null) {
                            googleStudioApiKey.setSocks5Id("");
                            throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                        }

                        // 更新次数
                        for (int j = 0; j < 10; j++) {
                            try {
                                googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(apiKeyId, task.getUserID());
                                googleStudioApiKey.setUseByMinute(googleStudioApiKey.getUseByMinute() + 1);
                                googleStudioApiKey.setUseByDay(googleStudioApiKey.getUseByDay() + 1);
                                googleStudioApiKeyRepository.update(googleStudioApiKey);
                                break;
                            } catch (Exception e) {
                                log.info("ImageRecognition ERROR update 1: {}", e.getMessage());
                            }
                            if (j == 9) {
                                throw new Exception("googleStudioApiKey更新失败");
                            }
                        }
                        flag = true;

                        String type = AiTypeEnums.GoogleStudio.getCode();
                        if (StringUtils.isNotEmpty(googleStudioApiKey.getType())) {
                            type = googleStudioApiKey.getType();
                        }

                        AiImageRecognition imageRecognition = applicationContext.getBean(type, AiImageRecognition.class);
                        results = imageRecognition.imageRecognition(socks5, imageUrls, googleStudioApiKey.getApiKey(), promptVersion, i == 1 ? "gemini-2.5-flash-lite" : googleStudioApiKey.getUseByDay() <= 1000 ? "gemini-2.5-flash-lite" : googleStudioApiKey.getUseByDay() > 1200 ? "gemini-2.5-pro":"gemini-2.5-flash", "image/jpeg");
                        if (results == null || results.size() != imageUrls.size()) {
                            throw new Exception("识别失败，数量不一致");
                        }
                        Map<String, List<GoogleGenAiUtils.Result>> map = new HashMap<>();
                        for (int j = 0; j < imageUrls.size(); j++) {
                            List<GoogleGenAiUtils.Result> resultList = results.get(j);
                            String url = imageUrls.get(j);
                            if (url != null && resultList != null) {
                                resultList.forEach(e -> { if (e != null) e.setUrl(url);});
                            }
                            map.put(String.valueOf(j), resultList);
                        }
                        // 成功使用识别
                        for (int j = 0; j < 10; j++) {
                            try {
                                googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(apiKeyId, task.getUserID());
                                if (googleStudioApiKey != null) {
                                    googleStudioApiKey.setUsedSuccess(googleStudioApiKey.getUsedSuccess() + 1);
                                    googleStudioApiKey.setUsedSuccessByDay(googleStudioApiKey.getUsedSuccessByDay() + 1);
                                    Date lastSuccessTime = googleStudioApiKey.getLastSuccessTime();
                                    if (lastSuccessTime != null) {
                                        googleStudioApiKey.setTwoSuccessInterval((int) ((new Date().getTime() - lastSuccessTime.getTime()) / 1000));
                                    }
                                    googleStudioApiKey.setLastSuccessTime(new Date());
                                    googleStudioApiKeyRepository.update(googleStudioApiKey);
                                    wt = subTaskRepository.findById(task.get_id());
                                    this.reportTaskStatus(wt, SubTaskStatusEnums.success, map);
                                    return true;
                                }
                            } catch (Exception e) {
                                log.info("ImageRecognition ERROR update 2: {}", e.getMessage());
                            }
                        }
                        results = null;
                    } catch (CommonException e) {
                        throw e;
                    } catch (BeansException e) {
                        results = null;
                        // 找不到Bean 重新分配
                        log.info("ImageRecognition BeansException: {}", e.getMessage());
                        break;
                    } catch (Exception e) {
                        if (e.getMessage().equals("timeout") && i == 4) {
                            wt = subTaskRepository.findById(task.get_id());
                            this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "无法识别图片 timeout");
                            log.info("taskId {} accid {} status {} timeout failed", task.get_id(), task.getAccid(), wt.getStatus());
                            return false;
                        } else {
                            results = null;
                            log.info("ImageRecognition ERROR: {}", e.getMessage());
                            Thread.sleep(1000);
                        }
                    }
                }
                if (results == null) {
                    // 重新分配
                    wt = subTaskRepository.findById(task.get_id());
                    wt.setStatus(SubTaskStatusEnums.processing.getCode());
                    wt.setAccid("");
                    wt.getParams().put("apiKeyId", "");
                    wt.getParams().put("retry", MapUtils.getInteger(task.getParams(), "retry", 0) + 1);
                    subTaskRepository.save(wt);
                    log.info("taskId {} accid {} status {} 没有结果重新发布", task.get_id(), task.getAccid(), wt.getStatus());
                }
            } catch (Exception e) {
                if (e instanceof CommonException && (e.getMessage().contains("403") || e.getMessage().equals("API key not valid"))) {
                    googleStudioApiKeyRepository.updateDisable(apiKeyId);
                }
                if (e instanceof CommonException && (e.getMessage().contains("429") || e.getMessage().contains("503") || e.getMessage().contains("500"))) {
                    // 429 重新分配
                    wt = subTaskRepository.findById(task.get_id());
                    wt.setStatus(SubTaskStatusEnums.processing.getCode());
                    wt.setAccid("");
                    wt.getParams().put("apiKeyId", "");
                    if (!e.getMessage().contains("429")) {
                        wt.getParams().put("retry", MapUtils.getInteger(task.getParams(), "retry", 0) + 1);
                    }
                    subTaskRepository.save(wt);
                    log.info("taskId {} accid {} status {} 429/503重新发布", task.get_id(), task.getAccid(), wt.getStatus());
                    if (e.getMessage().contains("429")) {
                        // 达到限制，当天不可用
//                        int tooManyWaitSecond = Integer.parseInt(paramsService.getParams("account.tooManyWaitSecond", null, null).toString());
                        if (!e.getMessage().contains("retryDelay")) {
//                            GoogleStudioApiKey googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(apiKeyId, task.getUserID());
//                            googleStudioApiKey.setUseByDay(googleStudioApiKey.getLimitByDay());
//                            googleStudioApiKeyRepository.update(googleStudioApiKey);
                            waitInterval = 60*60; // 10分钟不可用
                        } else {
                            waitInterval = 60*60; // 1分钟不可用
                        }
                    }
                } else {
                    if (e.getMessage().contains("User location is not supported for the API use.") || e.getMessage().contains("403")) {
                        // 重新分配
                        wt = subTaskRepository.findById(task.get_id());
                        wt.setStatus(SubTaskStatusEnums.processing.getCode());
                        wt.setAccid("");
                        wt.getParams().put("apiKeyId", "");
                        subTaskRepository.save(wt);
                        log.info("taskId {} accid {} status {} User location/403重新发布", task.get_id(), task.getAccid(), wt.getStatus());
                        waitInterval = 10; // 出现错误优先级 下降
                    } else {
                        if (e.getMessage().contains("GOOGLE AI 审核提示：含有禁止内容")) {
                            wt = subTaskRepository.findById(task.get_id());
                            this.reportTaskStatus(wt, SubTaskStatusEnums.failed, e.getMessage());
                            log.info("taskId {} accid {} status {} failed", task.get_id(), task.getAccid(), wt.getStatus());
                        } else {
                            // 重新分配
                            wt = subTaskRepository.findById(task.get_id());
                            wt.setStatus(SubTaskStatusEnums.processing.getCode());
                            wt.setAccid("");
                            wt.getParams().put("apiKeyId", "");
                            wt.getParams().put("retry", Integer.parseInt(wt.getParams().getOrDefault("retry", "0").toString()) + 1);
                            subTaskRepository.save(wt);
                            log.info("taskId {} accid {} status {} User {} 重新发布", task.get_id(), task.getAccid(), wt.getStatus(), e.getMessage());
                            waitInterval = 10; // 出现错误优先级 下降
                        }
                    }
                }
            }
        } finally {
            if (StringUtils.isNotEmpty(apiKeyId)) {
                GoogleStudioApiKey googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(apiKeyId, task.getUserID());
                if (googleStudioApiKey != null) {
                    if (waitInterval >= 60 * 60) {
                        int tooManyWaitSecond = Integer.parseInt(paramsService.getParams("account.tooManyWaitSecond", null, null).toString());

                        if (googleStudioApiKey.getConsecutiveFailureCount() == null) {
                            googleStudioApiKey.setConsecutiveFailureCount(1);
                        } else {
                            googleStudioApiKey.setConsecutiveFailureCount(googleStudioApiKey.getConsecutiveFailureCount() + 1);
                        }
                        waitInterval = tooManyWaitSecond * googleStudioApiKey.getConsecutiveFailureCount();
                    }
                    // 修改google下次使用时间
                    Calendar instance = Calendar.getInstance();
                    instance.add(Calendar.SECOND, waitInterval);
                    googleStudioApiKey.setNextUseTime(flag ? instance.getTime() : new Date());
                    if (!wt.getStatus().equals(SubTaskStatusEnums.success.getCode())) {
                        // 释放IP
                        socks5Service.releaseSocks5(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null);
                        googleStudioApiKey.setSocks5Id("");
                    } else {
                        // 清空连续失败值
                        googleStudioApiKey.setConsecutiveFailureCount(0);
                    }
                    googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), googleStudioApiKey.getNextUseTime(), googleStudioApiKey.getConsecutiveFailureCount());
//                googleStudioApiKeyRepository.update(googleStudioApiKey);
                }
            }
        }

        return false;
    }
}
