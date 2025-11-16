package com.nyy.gmail.cloud.tasks.impl;

import com.alibaba.fastjson2.JSONArray;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.framework.mq.SubTaskMQProducer;
import com.nyy.gmail.cloud.gateway.GatewayClient;
import com.nyy.gmail.cloud.gateway.dto.SendEmailResponse;
import com.nyy.gmail.cloud.model.dto.AccountListDTO;
import com.nyy.gmail.cloud.mq.entity.TaskMessage;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.service.ParamsService;
import com.nyy.gmail.cloud.service.ProxyAccountService;
import com.nyy.gmail.cloud.service.Socks5Service;
import com.nyy.gmail.cloud.service.ai.AiImageRecognition;
import com.nyy.gmail.cloud.tasks.AbstractTask;
import com.nyy.gmail.cloud.tasks.BaseTask;
import com.nyy.gmail.cloud.utils.*;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.nyy.gmail.cloud.service.GetCodeService.filterHtmlUsingRegex;
import static com.nyy.gmail.cloud.utils.HttpUtil.cutBefore;

@Slf4j
@Component("BatchSendEmailTestV2")
public class BatchSendEmailTestV2TaskImpl extends AbstractTask implements BaseTask {
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

    private Map<String, List<String>>  aiContentCache = new HashMap<>();

    private Map<String, BlockingQueue<String>>  aiContentCacheV2 = new ConcurrentHashMap<>();

    @Autowired
    private SendEmailEventMonitorRepository sendEmailEventMonitorRepository;

    @Autowired
    private GatewayClient gatewayClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceDetailRepository balanceDetailRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private AccountGroupRepository accountGroupRepository;

    @Autowired
    @Qualifier("aiExecutor")
    private Executor aiExecutor;

    @Override
    protected SubTaskRepository getSubTaskRepository() {
        return subTaskRepository;
    }

    @Override
    protected TaskUtil getTaskUtil() {
        return taskUtil;
    }

    private static final ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();

    @Override
    public boolean publishTask(GroupTask groupTask) {
        String userID = groupTask.getUserID();
        Map params = groupTask.getParams();
        groupTask.setUpdateTime(new Date());

        String sendEmailMaxNumByDay = paramsService.getParams("account.sendEmailMaxNumByDay", null, null).toString();

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
                    PageResult<SubTask> subTaskPageResult = subTaskRepository.findByPagination(100, 1, new HashMap<>(Map.of("status", SubTaskStatusEnums.success.getCode(), "type", TaskTypesEnums.BatchSendEmail.getCode())), new HashMap<>());
                    List<String> titles = subTaskPageResult.getData().stream().map(e -> e.getParams().getOrDefault("title", "title").toString()).toList();
                    List<String> contents = subTaskPageResult.getData().stream().map(e -> e.getParams().getOrDefault("content", "content").toString()).toList();

                    for (int i = 0; i < addDatas.size(); i ++) {
                        if (StringUtils.isEmpty(addDatas.get(i))) {
                            continue;
                        }
                        String content = contents.getFirst();
                        String title = titles.getFirst();
                        if (!aiContentCacheV2.containsKey(content) || aiContentCacheV2.get(content).isEmpty()) {
                            contents = contents.subList(1, contents.size());
                            titles = titles.subList(1, titles.size());
                            content = contents.getFirst();
                            title = titles.getFirst();
                        }

                        String addData = addDatas.get(i);
                        String[] split = addData.split("----");
                        SubTask subTask = taskUtil.newTaskModel(groupTask, Map.of(), new HashMap(Map.of(
                                "email", split[0].trim(), "title", title, "addData", addData,"sendEmailIntervalInSecond", params.getOrDefault("sendEmailIntervalInSecond","5"))), new Date());
                        subTask.setTmpId(UUIDUtils.get32UUId());
                        // 替换
                        String thisContent = content;
//                        thisContent = aiOptimizeV2(thisContent, groupTask.getUserID());
                        subTask.getParams().put("content", thisContent);

                        processingTasks.add(subTask);
                        taskCount++;
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

        List<String> ids = groupTask.getIds();
        int onceMaxCount = 500;

        List<TaskMessage> mqMsg = new ArrayList<>();
        int count = 0;
        Map<String, Account> accountMap = new HashMap<>();
        List<String> exceptIds = new ArrayList<>();

        List<SubTask> subTaskList = subTaskRepository.findByUserIDEqualsAndGroupTaskIdEqualsAndStatusIn(userID, groupTask.get_id(), List.of(SubTaskStatusEnums.processing.getCode()), 1, onceMaxCount);

        Collections.shuffle(ids);
        for (SubTask subTask : subTaskList) {
            if (ids.isEmpty()) {
                this.reportTaskStatus(subTask, SubTaskStatusEnums.failed, "没有账号");
                continue;
            }
            // 随机
            String accid = ids.get(count % ids.size());
            count++;
            if (exceptIds.contains(accid)) {
                continue;
            }
            Account account = accountMap.get(accid);
            if (account == null) {
                account = accountRepository.findById(accid);
            }
            if (account == null || !account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode())) {
                if (!exceptIds.contains(accid)) {
                    log.info("groupTaskId: {}, accid: {} 不在线、触发限制排除", groupTask.get_id(), accid);
                    exceptIds.add(accid);
                }
                continue;
            } else {
                // 限制单账号每天最多发送
                long sendNum = subTaskRepository.countSendEmailOneDayByAccid(accid);
                if (sendNum >= Long.parseLong(sendEmailMaxNumByDay)) {
                    if (!exceptIds.contains(accid)) {
                        log.info("groupTaskId: {}, accid: {} 发送数量 超过限制 {}, 实际：{}", groupTask.get_id(), accid, sendEmailMaxNumByDay, sendNum);
                        exceptIds.add(accid);
                    }
                    continue;
                }
                accountMap.put(accid, account);
            }
            sendEmailEventMonitorRepository.updateSubTaskIdAndGroupTaskId(subTask.getTmpId(), subTask.get_id(), subTask.getGroupTaskId(), subTask.getUserID());

            taskUtil.distributeAccount(account, subTask);

            mqMsg.add(taskUtil.domain2mqTask(subTask));
        }

        if (!mqMsg.isEmpty()) {
            subTaskMQProducer.sendMessage(mqMsg);
        }

        log.info("groupTaskId: {} exceptIds size: {}, ids size: {}, mqMsg size: {}", groupTask.get_id(), exceptIds.size(), ids.size(), mqMsg.size());
        if (!exceptIds.isEmpty()) {
            for (int i = 0; i < 10; i++) {
                try {
                    ids = ids.stream().filter(e -> !exceptIds.contains(e)).toList();
                    groupTask.setIds(ids);
                    groupTaskRepository.save(groupTask);
                    break;
                } catch (Exception ex) {
                    if (groupTask != null) {
                        groupTask = groupTaskRepository.findById(groupTask.get_id()).orElse(null);
                        ids = groupTask.getIds();
                    }
                }
            }
        }

        try {
            if (subTaskList != null) {
                if (subTaskList.isEmpty() && groupTask.getSuccess() + groupTask.getFailed() >= groupTask.getTotal()) {
                    // 垃圾邮件检测结果
                    long success = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.success.getCode());
                    long failed = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.failed.getCode());
                    groupTask.setSuccess(success);
                    groupTask.setFailed(failed);
                    if (groupTask.getSuccess() + groupTask.getFailed() >= groupTask.getTotal()) {
                        if (checkResult(groupTask)) {
                            groupTask.setPublishStatus("success");
                            groupTask.setFinishTime(new Date());
                            if (!groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode())) {
                                groupTask.setStatus(GroupTaskStatusEnums.success.getCode());
                            }
                            groupTaskRepository.save(groupTask);
                        }
                    } else {
                        groupTask.setStatus(GroupTaskStatusEnums.init.getCode());
                        groupTaskRepository.save(groupTask);
                    }
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
                    }
                    groupTaskRepository.save(groupTask);
                }
            }

            if (!subTaskList.isEmpty()) {
                long success = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.success.getCode());
                long failed = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.failed.getCode());
                groupTask.setSuccess(success);
                groupTask.setFailed(failed);
                groupTaskRepository.save(groupTask);
            }
        } catch (OptimisticLockingFailureException e) {
            log.info("{} OptimisticLockingFailureException: {}", groupTask.get_id(), e.getMessage());
        }
        return true;
    }

    private boolean checkResult(GroupTask groupTask) {
        // 30s没有收到任务垃圾邮件、任务结束
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.HOUR_OF_DAY, -1);

        Calendar instance1 = Calendar.getInstance();
        instance1.add(Calendar.SECOND, -30);
        List<SubTask> subTasks = subTaskRepository.findByGroupTaskId(groupTask.get_id());
        for (SubTask subTask : subTasks) {
            if (subTask.getStatus().equals(SubTaskStatusEnums.success.getCode()) && subTask.getResult() == null) {
                String email = subTask.getParams().getOrDefault("email", "").toString();
                Account account = accountRepository.findByEmail(email);
                Account account1 = accountRepository.findById(subTask.getAccid());
                if (account != null && account1 != null) {
                    List<Message> messageList = messageRepository.findByAccIdAndSenderAndCreateTimeGatherThan(account.get_id(), List.of(account1.getEmail()), subTask.getFinishTime());
                    if (!messageList.isEmpty()) {
                        boolean match = messageList.stream().anyMatch(e -> e.getLabels() != null && e.getLabels().contains("^s"));
                        if (match) {
                            subTask.setResult(Map.of("msg", "收到垃圾邮件"));
                            subTaskRepository.save(subTask);
                        } else {
                            subTask.setResult(Map.of("msg", "收到邮件"));
                            subTaskRepository.save(subTask);
                        }
                        accountRepository.updateLimitSendEmail(account1.get_id(), false);
                    }
                    if (subTask.getFinishTime().after(instance1.getTime())) {
                        // 刷新获取邮件
                        accountRepository.updateLastPullMessageTime(account.get_id(), instance.getTime());
                    } else {
                        subTask.setResult(Map.of("msg", "超时未收到邮件"));
                        subTaskRepository.save(subTask);
                    }
                } else {
                    subTask.setResult(Map.of("msg", "邮箱账号不存在"));
                    subTaskRepository.save(subTask);
                }
            }
        }

        int success = 0;
        int failed = 0;
        int successB = 0;
        int failedB = 0;
        int totalA = 0;
        int totalB = 0;
        for (SubTask subTask : subTasks) {
            if (subTask.getStatus().equals(SubTaskStatusEnums.success.getCode())) {
                String isB = subTask.getParams().getOrDefault("isB", "0").toString();
                if (isB.equals("1")) {
                    totalB ++;
                } else {
                    totalA ++;
                }
                if (subTask.getResult() != null) {
                    String msg = subTask.getResult().getOrDefault("msg", "").toString();
                    if (msg.equals("收到邮件")) {
                        if (isB.equals("1")) {
                            successB ++;
                        } else {
                            success ++;
                        }
                    } else {
                        if (isB.equals("1")) {
                            failedB ++;
                        } else {
                            failed ++;
                        }
                    }
                }
            }
        }
        if (totalB + totalA == success + failed + successB + failedB) {
            groupTask.setResult(Map.of("successA", success, "failedA", failed, "successB", successB, "failedB", failedB));
            return true;
        }
        return false;
    }


    private void generateContent (String thisContent) {
        String sendEmailAiModel = paramsService.getParams("account.sendEmailAiModel", null, null).toString();
        AiTypeEnums type = AiTypeEnums.GoogleStudio;
        if (sendEmailAiModel.equalsIgnoreCase("chatgpt") || sendEmailAiModel.equalsIgnoreCase(AiTypeEnums.Chatgpt.getCode())) {
            type = AiTypeEnums.Chatgpt;
        }

        String apiKey = "AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE";
        int n = 20;
        if (thisContent.length() > 820) {
            n = 10;
        }
        for (int i = 0; i < 10; i++) {
            try {
                if (i > 0 && n > 10) {
                    n = 10;
                }
                AiImageRecognition imageRecognition = applicationContext.getBean(type.getCode(), AiImageRecognition.class);
                String contentOptimize = imageRecognition.emailContentOptimizeV2(thisContent, apiKey, n - i);
                if (StringUtils.isNotEmpty(contentOptimize)) {
                    contentOptimize = contentOptimize.trim();
                    if (contentOptimize.startsWith("```json")) {
                        contentOptimize =  contentOptimize.substring("```json".length());
                    } else if (contentOptimize.contains("```json")) {
                        contentOptimize = contentOptimize.split("```json")[1];
                    }
                    if (contentOptimize.startsWith("json")) {
                        contentOptimize =  contentOptimize.substring("json".length());
                    }
                    if (contentOptimize.endsWith("```")) {
                        contentOptimize = contentOptimize.substring(0, contentOptimize.length() - 3);
                    }
                    if (contentOptimize.contains("```")) {
                        contentOptimize = cutBefore(contentOptimize, "```");
                    }
                    JSONArray jsonArray = JSONArray.parseArray(contentOptimize);

                    BlockingQueue<String> queue = aiContentCacheV2.computeIfAbsent(thisContent, k -> new LinkedBlockingQueue<>());

                    for (int j = 0; j < jsonArray.size(); j++) {
                        queue.offer(jsonArray.getJSONObject(j).getString("email"));
                    }
                    break;
                }
            } catch (Exception e) {
                log.info("AI 生成出现异常", e);
            }
        }
    }

    private String aiOptimizeV2(String thisContent, String useID, String groupTaskId) {
        String s = filterHtmlUsingRegex(thisContent);
        if (StringUtils.isBlank(s)) {
            return thisContent;
        }
        Object lock = LOCKS.computeIfAbsent(thisContent, k -> new Object());
        synchronized (lock) {
            try {
                if (aiContentCacheV2.size() > 10000) {
                    int half = aiContentCacheV2.size() / 2;
                    Iterator<Map.Entry<String, BlockingQueue<String>>> iterator = aiContentCacheV2.entrySet().iterator();
                    for (int i = 0; i < half && iterator.hasNext(); i++) {
                        iterator.next();
                        iterator.remove();
                    }
                }
                BlockingQueue<String> queue = aiContentCacheV2.get(thisContent);
                if (queue != null) {
                    String result = queue.poll();
                    log.info("是否有缓存：" + (result != null));
                    if (StringUtils.isNotBlank(result)) {
                        // 数量不足 20 去生成
                        if (queue.size() < 20) {
                            // 判断SubTask有多少
                            long count = subTaskRepository.countBySendEmailContent(thisContent, groupTaskId);
                            int n = 20;
                            if (thisContent.length() > 820) {
                                n = 10;
                            }
                            count = count / n;
                            for (int i = 0; i < Math.min(count, 10); i++) {
                                aiExecutor.execute(() -> generateContent(thisContent));
                            }
                        }
                        return result;
                    }
                    aiContentCacheV2.remove(thisContent);
                } else {
                    log.info("是否有缓存：false");
                }

                generateContent(thisContent);
                queue = aiContentCacheV2.get(thisContent);
                String poll = queue == null ? null : queue.poll();
                return poll == null ? thisContent : poll;
            } catch (Exception e) {
                return thisContent;
            } finally {
                LOCKS.remove(thisContent, lock);
            }
        }
    }

    private void generateTitle (String title) {
        String sendEmailAiModel = paramsService.getParams("account.sendEmailAiModel", null, null).toString();
        AiTypeEnums type = AiTypeEnums.GoogleStudio;
        if (sendEmailAiModel.equalsIgnoreCase("chatgpt") || sendEmailAiModel.equalsIgnoreCase(AiTypeEnums.Chatgpt.getCode())) {
            type = AiTypeEnums.Chatgpt;
        }

        String apiKey = "AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE";
        int n = 50;
        if (title.length() > 50) {
            n = 20;
        }
        for (int i = 0; i < 10; i++) {
            try {
                if (i > 0 && n > 20) {
                    n = 20;
                }
                AiImageRecognition imageRecognition = applicationContext.getBean(type.getCode(), AiImageRecognition.class);
                String contentOptimize = imageRecognition.emailTitleOptimizeV2(title, apiKey, n - i);
                if (StringUtils.isNotEmpty(contentOptimize)) {
                    contentOptimize = contentOptimize.trim();
                    if (contentOptimize.startsWith("```json")) {
                        contentOptimize =  contentOptimize.substring("```json".length());
                    } else if (contentOptimize.contains("```json")) {
                        contentOptimize = contentOptimize.split("```json")[1];
                    }
                    if (contentOptimize.startsWith("json")) {
                        contentOptimize =  contentOptimize.substring("json".length());
                    }
                    if (contentOptimize.endsWith("```")) {
                        contentOptimize = contentOptimize.substring(0, contentOptimize.length() - 3);
                    }
                    if (contentOptimize.contains("```")) {
                        contentOptimize = cutBefore(contentOptimize, "```");
                    }
                    JSONArray jsonArray = JSONArray.parseArray(contentOptimize);

                    BlockingQueue<String> queue = aiContentCacheV2.computeIfAbsent(title, k -> new LinkedBlockingQueue<>());

                    for (int j = 0; j < jsonArray.size(); j++) {
                        queue.offer(jsonArray.getJSONObject(j).getString("title"));
                    }
                    break;
                }
            } catch (Exception e) {
                log.info("AI 生成出现异常", e);
            }
        }
    }


    private String aiOptimizeTitle(String title, String useID, String groupTaskId) {
        Object lock = LOCKS.computeIfAbsent(title, k -> new Object());
        synchronized (lock) {
            try {
                if (aiContentCacheV2.size() > 10000) {
                    int half = aiContentCacheV2.size() / 2;
                    Iterator<Map.Entry<String, BlockingQueue<String>>> iterator = aiContentCacheV2.entrySet().iterator();
                    for (int i = 0; i < half && iterator.hasNext(); i++) {
                        iterator.next();
                        iterator.remove();
                    }
                }

                BlockingQueue<String> queue = aiContentCacheV2.get(title);
                if (queue != null) {
                    String result = queue.poll();
                    log.info("是否有缓存：" + (result != null));
                    if (StringUtils.isNotBlank(result)) {
                        // 数量不足 20 去生成
                        if (queue.size() < 50) {
                            // 判断SubTask有多少
                            long count = subTaskRepository.countBySendEmailTitle(title, groupTaskId);
                            int n = 50;
                            if (title.length() > 50) {
                                n = 20;
                            }
                            count = count / n;
                            for (int i = 0; i < Math.min(count, 10); i++) {
                                aiExecutor.execute(() -> generateTitle(title));
                            }
                        }
                        return result;
                    }
                    aiContentCacheV2.remove(title);
                } else {
                    log.info("是否有缓存：false");
                }

                generateTitle(title);
                queue = aiContentCacheV2.get(title);
                String poll = queue == null ? null : queue.poll();
                return poll == null ? title : poll;
            } catch (Exception e) {
                return title;
            } finally {
                LOCKS.remove(title, lock);
            }
        }
    }

    @Override
    public boolean checkTask(SubTask task, Account account, Date now) {
        // 锁定账号
        if (account.getType() != null && account.getType().equals(AccountTypeEnums.web.getCode())) {
            RBucket<String> bucket = redissonClient.getBucket("CookieGatewayClientLock");
            if (bucket.isExists()) {
                return false;
            }
        }
        return redisUtil.getLock("SendEmail:" + account.get_id(), "1", 300, TimeUnit.SECONDS);
    }

    private boolean accountOfflineInitSubTask(SubTask task) {
        Account account = accountRepository.findById(task.getAccid());
        if (account == null || account.getOnlineStatus().equals(AccountOnlineStatus.OFFLINE.getCode()) || account.getLimitSendEmail()) {
            retrySubTask(task);
            return true;
        }
        return false;
    }

    private void retrySubTask(SubTask task) {
        for (int i = 0; i < 10; i++) {
            try {
                task.setStatus(SubTaskStatusEnums.processing.getCode());
                task.setAccid("");
                subTaskRepository.save(task);
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                task = subTaskRepository.findById(task.get_id());
            }
        }
    }

    @Override
    public boolean runTask(SubTask task, Account account) {
        SubTask wt = null;
        try {
            wt = subTaskRepository.findById(task.get_id());
            if (wt == null) {
                log.info("id {} 任务不存在", task.get_id());
                return false;
            }
            if (!wt.getStatus().equals(SubTaskStatusEnums.init.getCode())) {
                log.info("id {} 任务状态不正确", task.get_id());
                return false;
            }
            GroupTask groupTask = groupTaskRepository.findById(task.getGroupTaskId()).orElse(null);
            if (groupTask == null) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "任务已经结束");
                return false;
            }
            String tuiRetry = task.getParams().getOrDefault("tuiRetry", "").toString();
            if (StringUtils.isEmpty(tuiRetry)) {
                if (groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode())) {
                    this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "任务已经结束");
                    return false;
                }
            }
            if (account == null || !account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode())) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "账号不可用");
                return false;
            }
            if (account.getType() != null && account.getType().equals(AccountTypeEnums.web.getCode())) {
                RBucket<String> bucket = redissonClient.getBucket("CookieGatewayClientLock");
                if (bucket.isExists()) {
                    retrySubTask(task);
                    return false;
                }
            }

            String email = task.getParams().getOrDefault("email", "").toString();
            String title = task.getParams().getOrDefault("title", "").toString();
            String content = task.getParams().getOrDefault("content", "").toString();
            String addData = task.getParams().getOrDefault("addData", "").toString();

            String[] split = addData.split("----");
            Map<String, String> map = new HashMap<>();
            for (int j = 1; j < split.length; j++) {
                map.put("var" + j, split[j]);
            }
            if (content.equals("邮件内容过大被TNT屏蔽")) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "邮件内容过大被TNT屏蔽");
                return false;
            }
//            try {
//                String thisContent = content;
//                content = aiOptimizeV2(thisContent, groupTask.getUserID(), groupTask.get_id());
//            } catch (Exception e) {
//                log.info("修改邮件内容和标题失败", e);
//            }
            wt.getParams().put("sendEmail", account.getEmail());

            SendEmailResponse sendEmailResponse = gatewayClient.sendEmail(title, account.getEmail(), List.of(email), content, account);
            if (sendEmailResponse != null && sendEmailResponse.getCode().equals(0)) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.success, null);
            } else {
                if (!accountOfflineInitSubTask(task)) {
                    if (sendEmailResponse != null && sendEmailResponse.getMsg().contains("ProxyTimeoutError")) {
                        retrySubTask(task);
                    } else {
                        this.reportTaskStatus(wt, SubTaskStatusEnums.failed, sendEmailResponse == null ? "请求未返回结果" : sendEmailResponse.getMsg());
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof CommonException && ((CommonException) e).getCode() == ResultCode.INSUFFICIENT_USER_BALANCE.getCode()) {
                retrySubTask(task);
                return false;
            }
            if (wt != null && !accountOfflineInitSubTask(wt)) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "发送邮件错误：" + e.getMessage());
            }
        } finally {
            if (account != null) {
                String sendEmailIntervalInSecond = paramsService.getParams("account.sendEmailIntervalInSecond", null, null).toString();
                sendEmailIntervalInSecond = task.getParams().getOrDefault("sendEmailIntervalInSecond", sendEmailIntervalInSecond).toString();
                try {
                    String[] parts = sendEmailIntervalInSecond.split("~", -1);
                    if (parts.length >= 2) {
                        int min = Integer.parseInt(parts[0]);  // 最小值
                        int max = Integer.parseInt(parts[1]);  // 最大值
                        Random random = new Random();
                        int randomNumber = random.nextInt((max - min) + 1) + min;
                        sendEmailIntervalInSecond = randomNumber + "";
                    } else {
                        Integer.parseInt(sendEmailIntervalInSecond);
                    }
                } catch (Exception e) {
                    int min = 5;  // 最小值
                    int max = 10;  // 最大值
                    Random random = new Random();
                    int randomNumber = random.nextInt((max - min) + 1) + min;
                    sendEmailIntervalInSecond = randomNumber + "";
                }

                redisUtil.set("SendEmail:" + account.get_id(), "1",  Integer.parseInt(sendEmailIntervalInSecond));
            }
            if (wt != null && wt.getStatus().equals(SubTaskStatusEnums.failed.getCode())) {
                // 退款
                try {
                    releaseSendEmailBalance(wt.getUserID());
                } catch (Exception e) {
                    log.info("发送邮件失败后释放余额失败，错误：" + e.getMessage());
                }
            }
        }
        return false;
    }

    private void releaseSendEmailBalance(String userID) throws InterruptedException {
        RLock lock = redissonClient.getLock(userID + "-charge");
        if (lock.tryLock(30, TimeUnit.SECONDS)) {
            try {
                User user = userRepository.findOneByUserID(userID);
                if (user != null) {
                    user.setRestSendEmailCount(user.getRestSendEmailCount().add(BigDecimal.ONE));
                    userRepository.updateUser(user);

                    balanceDetailRepository.addUserBill("邮件发送失败退款",
                            userID, BigDecimal.ONE, user.getRestSendEmailCount(), user.getName(), BillExpenseTypeEnums.IN, BillCateTypeEnums.RESTORE_SEND_EMAIL_COUNT);
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
