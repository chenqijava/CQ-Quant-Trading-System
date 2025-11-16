package com.nyy.gmail.cloud.tasks.impl;

import com.alibaba.fastjson2.JSONArray;
import com.nyy.gmail.cloud.common.configuration.PathConfig;
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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.nyy.gmail.cloud.service.GetCodeService.filterHtmlUsingRegex;
import static com.nyy.gmail.cloud.utils.HttpUtil.cutBefore;

@Slf4j
@Component("EmailLinkCheck")
public class EmailLinkCheckTaskImpl extends AbstractTask implements BaseTask {
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

    @Autowired
    private PathConfig pathConfig;

    @Override
    protected SubTaskRepository getSubTaskRepository() {
        return subTaskRepository;
    }

    @Override
    protected TaskUtil getTaskUtil() {
        return taskUtil;
    }

    private static final List<String> monitorEventList = List.of("open", "click", "reply");

    private static final ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();

    public static String insertRandomImgSafe(String html, String src) {
        if (html == null || html.isEmpty()) return src;

        String img = src;

        List<Integer> insertPositions = new ArrayList<>();

        // 扫描 HTML，找所有 "><" 之间的插入点
        for (int i = 0; i < html.length() - 1; i++) {
            if (html.charAt(i) == '>' && html.charAt(i + 1) != '<') {
                // i+1 是标签结束后的内容起点，可以插入
                insertPositions.add(i + 1);
            }
        }

        // 如果找不到合适位置，就插到 </body> 前 或者最后
        if (insertPositions.isEmpty()) {
            int bodyEnd = html.toLowerCase().indexOf("</body>");
            if (bodyEnd != -1) {
                return html.substring(0, bodyEnd) + img + html.substring(bodyEnd);
            }
            return html + img;
        }

        // 从合法插入点随机选择一个
        int pos = insertPositions.get(ThreadLocalRandom.current().nextInt(insertPositions.size()));
        return html.substring(0, pos) + img + html.substring(pos);
    }

    private String processEmailContent (String content, String uuid, String email, String monitorOpen, String monitorClick, String addUnsubscribe, String subTaskId) {
//        String baseUrl = paramsService.getParams("webConfig.baseUrl", null, null).toString();
        String baseUrl = getBaseUrl("img");
        String baseUrlExternal = getBaseUrl("external");
        // 提取文本中的所有 <a href="">
        for (String monitorEvent : monitorEventList) {
            if (monitorClick.equals("yes")) {
                if (monitorEvent.equals("click")) {
                    List<String> links = TemplateUtils.extractLinks(content);

                    SendEmailEventMonitor sendEmailEventMonitor = new SendEmailEventMonitor();
                    sendEmailEventMonitor.setEvent(monitorEvent);
                    sendEmailEventMonitor.setCount(0);
                    sendEmailEventMonitor.setUuid(uuid);
                    sendEmailEventMonitor.setCreateTime(new Date());
                    sendEmailEventMonitor.setLinkUrls(links.stream().collect(Collectors.toMap(MD5Util::MD5, e -> e, (k1, k2) -> k2)));
                    sendEmailEventMonitor.setEmail(email);
                    sendEmailEventMonitor.setSubTaskId(subTaskId);

                    sendEmailEventMonitorRepository.save(sendEmailEventMonitor);
                    // 所有链接替换
                    for (Map.Entry<String, String> entry : sendEmailEventMonitor.getLinkUrls().entrySet()) {
                        String pattern = "href=[\"']" + Pattern.quote(entry.getValue()) + "[\"']";
//                        String replacement = "href=\"" + baseUrlExternal + "/api/latest/external/" + uuid + "/" + entry.getKey() + "\"";
                        String replacement = "href=\"" + baseUrlExternal + "/" + uuid + "/" + entry.getKey() + "\"";
                        content = content.replaceAll(pattern, replacement);
                    }
                }
            }

            if (monitorOpen.equals("yes")) {
                if (monitorEvent.equals("open")) {
                    // 所有图片替换
                    List<String> imgs = TemplateUtils.extractImgSrc(content);
                    List<String> imgUrls = new ArrayList<>();
                    for (String img : imgs) {
                        String img1 = img;
                        if (img.startsWith("http")) {
                            try {
                                img1 = TemplateUtils.saveNetworkImage(img, FileUtils.resPath.resolve(Paths.get("img")).toFile());
                            } catch (IOException e) {
                            }
                        } else if (img.startsWith("data:image/")) {
                            try {
                                img1 = TemplateUtils.saveBase64Image(img, FileUtils.resPath.resolve(Paths.get("img")).toFile());
                            } catch (IOException e) {
                            }
                        }
                        if (!img.equals(img1)) {
                            String pattern = "src=[\"']" + Pattern.quote(img) + "[\"']";
//                            String replacement = "src=\"" + baseUrl + "/api/latest/img/uuid/" + img1 + "\"";
                            String replacement = "src=\"" + baseUrl + "/" + RandomStringUtils.randomAlphabetic(6) + "/" + img1 + "\"";
                            content = content.replaceAll(pattern, replacement);
                        }
                        imgUrls.add(img1);
                    }

                    SendEmailEventMonitor sendEmailEventMonitor = new SendEmailEventMonitor();
                    sendEmailEventMonitor.setEvent(monitorEvent);
                    sendEmailEventMonitor.setCount(0);
                    sendEmailEventMonitor.setUuid(uuid);
                    sendEmailEventMonitor.setCreateTime(new Date());
                    sendEmailEventMonitor.setImgUrls(imgUrls);
                    sendEmailEventMonitor.setEmail(email);
                    sendEmailEventMonitor.setSubTaskId(subTaskId);

                    sendEmailEventMonitorRepository.save(sendEmailEventMonitor);
                }
            }

            if (monitorEvent.equals("reply")) {
                SendEmailEventMonitor sendEmailEventMonitor = new SendEmailEventMonitor();
                sendEmailEventMonitor.setEvent(monitorEvent);
                sendEmailEventMonitor.setCount(0);
                sendEmailEventMonitor.setUuid(uuid);
                sendEmailEventMonitor.setCreateTime(new Date());
                sendEmailEventMonitor.setEmail(email);
                sendEmailEventMonitor.setSubTaskId(subTaskId);

                sendEmailEventMonitorRepository.save(sendEmailEventMonitor);
            }
        }

//        // 插入退订链接
//        if (addUnsubscribe.equals("yes")) {
//            content += "<hr />\n" +
//                    "<p style=\"font-size:12px; color:#888;\">\n" +
//                    "      If you no longer wish to receive these emails, you can \n" +
//                    "      <a href=\"" + baseUrl + "/api/latest/external/" + uuid + "/unsubscribe\" style=\"color:#888;\">unsubscribe here</a>.\n" +
//                    "    </p>";
//        }


        // 插入logo.jpg图片
        if (monitorOpen.equals("yes")) {
//            content = content + "\n" + "<img src=\"" + baseUrl + "/api/latest/img/" + uuid + "/logo.png\" width=\"1\" height=\"1\" style=\"display:none;\" alt=\"\" />\n";
            content = insertRandomImgSafe(content, "<img src=\"" + baseUrl + "/" + uuid + "/logo.png\" width=\"1\" height=\"1\" style=\"display:none;\" alt=\"\" />");
        }

//        content = "<html>\n" +
//                "  <head>\n" +
//                "    <style>\n" +
//                ".ql-editor ol { counter-reset: item; list-style: none; padding-left: 2em; }\n" +
//                ".ql-editor ol > li { counter-increment: item; position: relative; padding-left: 1.2em; margin-bottom: 6px; }\n" +
//                ".ql-editor ol > li::before { content: counters(item, \".\") \". \"; position: absolute; left: 0; color: #333; }\n" +
//                ".ql-editor ul { list-style-type: disc; }\n" +
//                ".ql-editor ul li { margin-bottom: 6px; list-style-type: disc; padding-left: 0; }" +
//                "    </style>\n" +
//                "  </head>\n" +
//                "  <body>\n" +
//                "    <div class=\"ql-editor\">" + content + " </div>\n" +
//                "  </body>\n" +
//                "</html>";
        return content;
    }

    @Override
    public boolean publishTask(GroupTask groupTask) {
        String userID = groupTask.getUserID();
        Map params = groupTask.getParams();
        groupTask.setUpdateTime(new Date());
        List<String> systemEmails = ((List<String>)params.get("systemEmail"));
        String accountUserID = (params.getOrDefault("accountUserID", Constants.ADMIN_USER_ID).toString());

        String sendEmailMaxNumByDay = paramsService.getParams("account.sendEmailMaxNumByDay", null, null).toString();
        String linkCheckTemplate = paramsService.getParams("account.linkCheckTemplate", null, null).toString();
        String linkCheckSubjectTemplate = paramsService.getParams("account.linkCheckSubjectTemplate", null, null).toString();


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
                    List<String> titles = new ArrayList<>();
                    List<String> contents = new ArrayList<>();
                    Pair<List<String>, Long> filepath = FileUtils.readFileLines(Path.of(pathConfig.getResBak(), linkCheckSubjectTemplate), 0, 100000);
                    titles = filepath.getLeft();
                    filepath = FileUtils.readFileLines(Path.of(pathConfig.getResBak(), linkCheckTemplate), 0, 100000);
                    contents = Arrays.stream(String.join("<br/>\n", filepath.getLeft()).split("==NEWLINE==")).filter(StringUtils::isNotBlank).toList();

                    for (int i = 0; i < addDatas.size(); i ++) {
                        if (StringUtils.isEmpty(addDatas.get(i))) {
                            continue;
                        }
                        String addData = addDatas.get(i);
                        for (String email : systemEmails) {
                            SubTask subTask = taskUtil.newTaskModel(groupTask, Map.of(), new HashMap(Map.of(
                                    "email", email, "title", titles.get((int) (taskCount % titles.size())), "addData", addData,
                                    "sendEmailIntervalInSecond", params.getOrDefault("sendEmailIntervalInSecond","5"))), new Date());
                            String thisContent = contents.get((int) (taskCount % contents.size()));
                            thisContent = processingBase64Img(thisContent);
                            Map<String, String> map = new HashMap<>();
                            map.put("NEWLINE", "<br/>\n");
                            map.put("contact", addData);
                            thisContent = TemplateUtils.replaceTemplateVars(thisContent, map);
                            subTask.getParams().put("content", thisContent);

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

        List<String> ids = groupTask.getIds();
        int onceMaxCount = 500;

        List<TaskMessage> mqMsg = new ArrayList<>();
        int count = 0;
        Map<String, Account> accountMap = new HashMap<>();
        List<String> exceptIds = new ArrayList<>();

        List<SubTask> subTaskList = subTaskRepository.findByUserIDEqualsAndGroupTaskIdEqualsAndStatusIn(userID, groupTask.get_id(), List.of(SubTaskStatusEnums.processing.getCode()), 1, onceMaxCount);
        if (!subTaskList.isEmpty() && ids.size() < 50 && subTaskList.size() > ids.size()) {
            // 可用账号不足，补充
            AccountListDTO accountListDTO = new AccountListDTO();
            accountListDTO.setFilters(new HashMap<>(Map.of("limitSendEmail", Map.of("$ne", true), "userID", accountUserID, "onlineStatus", AccountOnlineStatus.ONLINE.getCode(), "isCheck", true, "sendEmailNumByDay", Map.of("$lt", Integer.parseInt(sendEmailMaxNumByDay)))));
            AccountGroup group = accountGroupRepository.findByGroupNameAndUserID("群发邮件分组", accountUserID);
            if (group != null) {
                accountListDTO.getFilters().put("groupID", group.get_id());
            }
            String type = ((Map)params.get("reqDto")).getOrDefault("type", "direct").toString();
            if (type.equals("api")) {
                accountListDTO.getFilters().put("type", AccountTypeEnums.sendgrid.getCode());
            } else {
                accountListDTO.getFilters().put("type", Map.of("$ne", AccountTypeEnums.sendgrid.getCode()));
            }
            accountListDTO.setSorter(new HashMap<>(Map.of("lastPullMessageTime", -1)));
            PageResult<Account> accountPageResult = accountRepository.findByPagination(accountListDTO, Math.min(subTaskList.size(), 2000), 1);
            if (accountPageResult.getData().isEmpty()) {
                throw new CommonException(ResultCode.NO_ONLINE_ACCOUNT);
            }
            ids = accountPageResult.getData().stream().map(Account::get_id).collect(Collectors.toList());
            groupTask.setIds(ids);
        }

        Collections.shuffle(ids);// 打乱ID顺序
        for (SubTask subTask : subTaskList) {
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
            if (account == null || !account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode()) || account.getLimitSendEmail()) {
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
                // 单次任务发送次数限制
//                sendNum = subTaskRepository.countSendEmailOneGroupTaskByAccid(groupTask.get_id(), accid);
//                if (sendNum >= oneAccountSendLimit) {
//                    if (!exceptIds.contains(accid)) {
//                        log.info("groupTaskId: {}, accid: {} 单任务发送数量 超过限制 {}, 实际：{}", groupTask.get_id(), accid, oneAccountSendLimit, sendNum);
//                        exceptIds.add(accid);
//                    }
//                    continue;
//                }
                accountMap.put(accid, account);
            }

            taskUtil.distributeAccount(account, subTask);

            mqMsg.add(taskUtil.domain2mqTask(subTask));
        }

        if (!mqMsg.isEmpty()) {
            subTaskMQProducer.sendMessage(mqMsg);
        }

        log.info("groupTaskId: {} exceptIds size: {}, ids size: {}, mqMsg size: {} subTaskList size: {}", groupTask.get_id(), exceptIds.size(), ids.size(), mqMsg.size(), subTaskList.size());
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

    private String getBaseUrl (String type) {
        String baseUrl = paramsService.getParams("webConfig.baseUrl", null, null).toString();
        if (type.equals("img")) {
            String[] split = baseUrl.split("\\.");
            split[0] = split[0] + "-img";
            baseUrl = String.join(".", split);
            return baseUrl;
        } else {
            String[] split = baseUrl.split("\\.");
            split[0] = split[0] + "-external";
            baseUrl = String.join(".", split);
            return baseUrl;
        }
    }

    private String processingBase64Img(String content) {
        String baseUrl = getBaseUrl("img");

        // 所有图片替换
        List<String> imgs = TemplateUtils.extractImgSrc(content);
        for (String img : imgs) {
            String img1 = img;
            if (img.startsWith("http")) {
//                try {
//                    img1 = TemplateUtils.saveNetworkImage(img, FileUtils.resPath.resolve(Paths.get("img")).toFile());
//                } catch (IOException e) {
//                }
            } else if (img.startsWith("data:image/")) {
                for (int i = 0; i < 10; i++) {
                    try {
                        img1 = TemplateUtils.saveBase64Image(img, FileUtils.resPath.resolve(Paths.get("img")).toFile());
                        break;
                    } catch (IOException e) {
                        log.info("base64转图片失败", e);
                    }
                }
                if (img.equals(img1)) {
                    img1 = "1";
                }
            }
            if (!img.equals(img1)) {
                String pattern = "src=[\"']" + Pattern.quote(img) + "[\"']";
//                String replacement = "src=\"" + baseUrl + "/api/latest/img/uuid/" + img1 + "\"";
                String replacement = "src=\"" + baseUrl + "/" + RandomStringUtils.randomAlphabetic(6) + "/" + img1 + "\"";
                content = content.replaceAll(pattern, replacement);
            }
        }
        return content;
    }

    private boolean checkResult(GroupTask groupTask) {
        if (new Date().getTime() - groupTask.getCreateTime().getTime() > 3 * 3600 * 1000) {
            return true;
        }
        long count = subTaskRepository.countSuccessAndNotLabels(groupTask.get_id());
        if (count > 0) {
            return false;
        }
        return true;
    }

    private Map<String, String> selectContentAndTitle(GroupTask groupTask) {
        String content = ((Map)groupTask.getParams().get("reqDto")).get("content").toString();
        String title = ((Map)groupTask.getParams().get("reqDto")).get("title").toString();
        String contentB = ((Map)groupTask.getParams().get("reqDto")).getOrDefault("contentB", "").toString();
        String titleB = ((Map)groupTask.getParams().get("reqDto")).getOrDefault("titleB", "").toString();

        String factor = ((Map)groupTask.getParams().get("reqDto")).getOrDefault("factor", "").toString();
        List<SubTask> subTasks = subTaskRepository.findByGroupTaskId(groupTask.get_id());
        List<String> A = new ArrayList<>();
        List<String> B = new ArrayList<>();
        subTasks.forEach(subTask -> {
            if (subTask.getParams().getOrDefault("isB", "0").equals("1")) {
                B.add(subTask.get_id());
            } else {
                A.add(subTask.get_id());
            }
        });
        if (factor.equals("reply")) {
            long countA = sendEmailEventMonitorRepository.countBySubTaskIdInAndEventEqual(A, "reply");
            long countB = sendEmailEventMonitorRepository.countBySubTaskIdInAndEventEqual(B, "reply");
            if (countB > countA) {
                return Map.of("title", titleB, "content", contentB);
            } else {
                return Map.of("title", title, "content", content);
            }
        }
        if (factor.equals("open")) {
            long countA = sendEmailEventMonitorRepository.countBySubTaskIdInAndEventEqual(A, "open");
            long countB = sendEmailEventMonitorRepository.countBySubTaskIdInAndEventEqual(B, "open");
            if (countB > countA) {
                return Map.of("title", titleB, "content", contentB);
            } else {
                return Map.of("title", title, "content", content);
            }
        }
        if (factor.equals("click")) {
            long countA = sendEmailEventMonitorRepository.countBySubTaskIdInAndEventEqual(A, "click");
            long countB = sendEmailEventMonitorRepository.countBySubTaskIdInAndEventEqual(B, "click");
            if (countB > countA) {
                return Map.of("title", titleB, "content", contentB);
            } else {
                return Map.of("title", title, "content", content);
            }
        }

        return Map.of("title", title, "content", content);
    }

    private String aiOptimize(String thisContent, String useID) {
        if (aiContentCache.size() > 10000) {
            int half = aiContentCache.size() / 2;
            Iterator<Map.Entry<String, List<String>>> iterator = aiContentCache.entrySet().iterator();
            for (int i = 0; i < half && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }
        if (aiContentCache.containsKey(thisContent)) {
            int useCount = Integer.parseInt(aiContentCache.get(thisContent).getLast()) + 1;
            aiContentCache.get(thisContent).set(1, useCount + "");
            String result = aiContentCache.get(thisContent).getFirst();
            if (useCount >= 20) {
                aiContentCache.remove(thisContent);
            }
            return result;
        } else {
            GoogleStudioApiKey googleStudioApiKey = null;

            for (int i = 0; i < 10; i++) {
                int waitInterval = 10;
                try {
                    List<GoogleStudioApiKey> googleStudioApiKeyList = googleStudioApiKeyRepository.findByCanUsed(100, Constants.ADMIN_USER_ID, null, AiTypeEnums.GoogleStudio)
                            .stream().filter(e -> e.getUseByDay() < e.getLimitByDay() && e.getUseByMinute() < e.getLimitByMinute()).collect(Collectors.toList());
                    if (googleStudioApiKeyList.isEmpty()) {
                        return thisContent;
                    }
                    // 随机取一个
                    Collections.shuffle(googleStudioApiKeyList);
                    googleStudioApiKey = googleStudioApiKeyList.getFirst();

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
                    }
                    if (socks5 == null) {
                        throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                    }

                    // 更新次数
                    for (int j = 0; j < 10; j++) {
                        try {
                            googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                            googleStudioApiKey.setUseByMinute(googleStudioApiKey.getUseByMinute() + 1);
                            googleStudioApiKey.setUseByDay(googleStudioApiKey.getUseByDay() + 1);
                            googleStudioApiKeyRepository.update(googleStudioApiKey);
                            break;
                        } catch (Exception e) {
                            log.info("ImageRecognition ERROR update 1: {}", e.getMessage());
                        }
                    }

                    String type = AiTypeEnums.GoogleStudio.getCode();
                    if (StringUtils.isNotEmpty(googleStudioApiKey.getType())) {
                        type = googleStudioApiKey.getType();
                    }

                    AiImageRecognition imageRecognition = applicationContext.getBean(type, AiImageRecognition.class);
                    String contentOptimize = imageRecognition.emailContentOptimize(socks5, thisContent, googleStudioApiKey.getApiKey());
                    if (StringUtils.isNotEmpty(contentOptimize)) {
                        // 成功使用识别
                        for (int j = 0; j < 10; j++) {
                            try {
                                googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                                googleStudioApiKey.setUsedSuccess(googleStudioApiKey.getUsedSuccess() + 1);
                                googleStudioApiKey.setUsedSuccessByDay(googleStudioApiKey.getUsedSuccessByDay() + 1);
                                Date lastSuccessTime = googleStudioApiKey.getLastSuccessTime();
                                if (lastSuccessTime != null) {
                                    googleStudioApiKey.setTwoSuccessInterval((int) ((new Date().getTime() - lastSuccessTime.getTime()) / 1000));
                                }
                                googleStudioApiKey.setLastSuccessTime(new Date());
                                googleStudioApiKey.setConsecutiveFailureCount(0);
                                googleStudioApiKeyRepository.update(googleStudioApiKey);

                                aiContentCache.put(thisContent, new ArrayList<>(List.of(contentOptimize, "1")));

                                return contentOptimize;
                            } catch (Exception e) {
                                log.info("ImageRecognition ERROR update 2: {}", e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    if (googleStudioApiKey != null) {
                        // 释放IP
                        socks5Service.releaseSocks5(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null);
                        googleStudioApiKey.setSocks5Id("");

                        if (e instanceof CommonException && e.getMessage().contains("403")) {
                            googleStudioApiKeyRepository.updateDisable(googleStudioApiKey.get_id());
                        }
                        if (e instanceof CommonException && (e.getMessage().contains("429") || e.getMessage().contains("503"))) {
                            if (e.getMessage().contains("429")) {
                                waitInterval = 60*60; // 10分钟不可用
                            }
                        } else {
                            if (e.getMessage().contains("User location is not supported for the API use.") || e.getMessage().contains("403")) {
                                waitInterval = 10; // 出现错误优先级 下降
                            }
                        }
                    }
                } finally {
                    if (googleStudioApiKey != null) {
                        String socks5Id = googleStudioApiKey.getSocks5Id();
                        googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
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
                            googleStudioApiKey.setNextUseTime(instance.getTime());
                            googleStudioApiKey.setSocks5Id(socks5Id);

                            googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), googleStudioApiKey.getNextUseTime(), googleStudioApiKey.getConsecutiveFailureCount());
                        }
                    }
                }
            }

            return thisContent;
        }
    }

    private void generateContent (String thisContent) {
        String sendEmailAiModel = paramsService.getParams("account.sendEmailAiModel", null, null).toString();
        AiTypeEnums type = AiTypeEnums.GoogleStudio;
        if (sendEmailAiModel.equalsIgnoreCase("chatgpt") || sendEmailAiModel.equalsIgnoreCase(AiTypeEnums.Chatgpt.getCode())) {
            type = AiTypeEnums.Chatgpt;
        }

        String apiKey = "AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE";
        int n = 10;
        if (thisContent.length() > 820) {
            n = 5;
        }
        for (int i = 0; i < 5; i++) {
            try {
                if (i > 0 && n > 5) {
                    n = 5;
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
        return redisUtil.getLock("SendEmail:" + account.get_id(), "1",  300, TimeUnit.SECONDS);
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

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
            if (account == null || !account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode()) || account.getLimitSendEmail()) {
                retrySubTask(task);
                return false;
            }

            String email = task.getParams().getOrDefault("email", "").toString();
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "邮箱不合法");
                return false;
            }
            String title = task.getParams().getOrDefault("title", "").toString();
            String content = task.getParams().getOrDefault("content", "").toString();
            String addData = task.getParams().getOrDefault("addData", "").toString();

            String useAiOptimize = "enable";


            Map<String, String> map = new HashMap<>();
            map.put("NEWLINE", "<br/>\n");

            if (content.equals("邮件内容过大被TNT屏蔽")) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "邮件内容过大被TNT屏蔽");
                return false;
            }

            try {
                String thisContent = content;
                title = TemplateUtils.replaceTemplateVars(title, map);
                if (useAiOptimize.equals("enable")) {
                    thisContent = aiOptimizeV2(thisContent, groupTask.getUserID(), groupTask.get_id());
                    title = aiOptimizeTitle(title, groupTask.getUserID(), groupTask.get_id());
                }
                thisContent = TemplateUtils.replaceTemplateVars(thisContent, map);
                content = processEmailContent(thisContent, wt.getTmpId(), email, "", "", "", wt.get_id());
            } catch (Exception e) {
                log.info("修改邮件内容和标题失败", e);
            }

            wt.getParams().put("sendEmail", account.getEmail());

            SendEmailResponse sendEmailResponse = gatewayClient.sendEmail(title, account.getEmail(), List.of(email), content, account);
            if (sendEmailResponse != null && sendEmailResponse.getCode().equals(0)) {
                String msg = null;
                if (account.getType().equals(AccountTypeEnums.sendgrid.getCode()) && StringUtils.isNotEmpty(sendEmailResponse.getMsg())) {
                    msg = sendEmailResponse.getMsg();
                }
                updateTotalSendEmailCount(groupTask);
                this.reportTaskStatus(wt, SubTaskStatusEnums.success, msg);
            } else {
                if (!accountOfflineInitSubTask(task)) {
                    if (sendEmailResponse != null && (sendEmailResponse.getMsg().contains("ProxyTimeoutError") || sendEmailResponse.getMsg().contains("ConnectionResetError"))) {
                        retrySubTask(task);
                    } else {
                        if (sendEmailResponse == null) {
                            retrySubTask(task);
                        } else {
                            this.reportTaskStatus(wt, SubTaskStatusEnums.failed, sendEmailResponse == null ? "请求未返回结果" : sendEmailResponse.getMsg());
                        }
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

    private void updateTotalSendEmailCount(GroupTask groupTask) {
        for (int i = 0; i < 100; i++) {
            try {
                groupTask = groupTaskRepository.findById(groupTask.get_id()).orElse(null);
                if (groupTask != null) {
                    String totalSendEmailCount = groupTask.getParams().getOrDefault("totalSendEmailCount", "0").toString();
                    Integer count = Integer.parseInt(totalSendEmailCount) + 1;
                    groupTask.getParams().put("totalSendEmailCount", count);
                    groupTaskRepository.save(groupTask);
                }
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        }
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
