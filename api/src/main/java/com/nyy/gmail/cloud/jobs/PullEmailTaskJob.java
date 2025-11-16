package com.nyy.gmail.cloud.jobs;

import cn.hutool.core.bean.BeanUtil;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.Message;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.AccountTypeEnums;
import com.nyy.gmail.cloud.enums.SubTaskStatusEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.gateway.GatewayClient;
import com.nyy.gmail.cloud.gateway.OutlookGraphGatewayClient;
import com.nyy.gmail.cloud.gateway.SmtpGatewayClient;
import com.nyy.gmail.cloud.gateway.dto.*;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.MessageRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.service.ParamsService;
import com.nyy.gmail.cloud.utils.DateUtil;
import com.nyy.gmail.cloud.utils.RedisUtil;
import com.nyy.gmail.cloud.utils.SmtpMailUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PullEmailTaskJob {

    @Autowired
    @Qualifier("taskOtherExecutor")
    private Executor taskExecutor;

    @Autowired
    private ParamsService paramsService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GatewayClient gatewayClient;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private RedisUtil redisUtil;

    private volatile boolean is_running = false;

    @Autowired
    private OutlookGraphGatewayClient outlookGraphGatewayClient;

    @Autowired
    private SmtpGatewayClient smtpGatewayClient;

    @Async("pullEmail")
    @Scheduled(cron = "0/2 * * * * ?")
    public void run() {
        if (is_running) return;

        int checkAccountInterval = Integer.parseInt(paramsService.getParams("account.checkAccountInterval", null, null).toString());
        if (checkAccountInterval > 1_000_000) return;

        is_running = true;
        try {
            log.info("开始拉取邮件");

            initNullAccounts();
            List<String> exportedIds = processExportAccounts();
            exportedIds = processZhongziAccounts(exportedIds);
            List<Account> toPull = getRegularAccounts(checkAccountInterval, exportedIds);

            log.info("开始检查普通的账号: {}", toPull.size());
            pullMessage(toPull, false);

            log.info("结束拉取邮件");
        } catch (Exception e) {
            log.error("PullEmailTaskJob.run 异常", e);
        } finally {
            is_running = false;
        }
    }

    private List<String> processZhongziAccounts(List<String> exportedIds) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -1);
        List<SubTask> subTasks = subTaskRepository.findByTypeAndFinishTimeAndStatusForSendEmail(TaskTypesEnums.BatchSendEmail.getCode(), calendar.getTime(), SubTaskStatusEnums.success.getCode());
        List<SubTask> subTasks2 = subTaskRepository.findByTypeAndFinishTimeAndStatusForSendEmail(TaskTypesEnums.EmailLinkCheck.getCode(), calendar.getTime(), SubTaskStatusEnums.success.getCode());
        List<SubTask> subTaskList = new ArrayList<>();
        subTaskList.addAll(subTasks);
        subTaskList.addAll(subTasks2);
        List<String> emails = subTaskList.stream().map(e -> e.getParams().getOrDefault("email", "").toString()).toList();
        List<Account> accountList = accountRepository.findbyEmails(emails);
        accountList = accountRepository.findByIds(accountList.stream().map(Account::get_id).filter(e -> !exportedIds.contains(e)).collect(Collectors.toList()));
        pullMessage(accountList, true);
        accountList.forEach(e -> exportedIds.add(e.get_id()));
        return exportedIds;
    }

    private void initNullAccounts() {
        List<Account> accountList = accountRepository.findByLastPullMessageTimeIsNull();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);
        Date resetTime = cal.getTime();
        for (Account account : accountList) {
            accountRepository.updateLastPullMessageTime(account.get_id(), resetTime);
        }
    }

    private List<String> processExportAccounts() {
        List<Account> onlineAccounts = accountRepository.findAllByOnlineStatusOnlyId(AccountOnlineStatus.ONLINE.getCode());
        List<String> accIds = new ArrayList<>();
        if (!onlineAccounts.isEmpty()) {
            List<SubTask> subTasks = subTaskRepository.findByTypeAndAccIdIn(TaskTypesEnums.AccountExport.getCode(), onlineAccounts.stream().map(Account::get_id).toList(), 100);
            accIds.addAll(subTasks.stream().map(SubTask::getAccid).toList());
            List<Account> accounts = accountRepository.findByIds(accIds);
            log.info("开始检查导出的账号: {}", accounts.size());
            pullMessage(accounts, true);
        }
        return accIds;
    }

    private List<Account> getRegularAccounts(int interval, List<String> excludeIds) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -interval);
        List<Account> candidates = accountRepository.findByLastPullMessageTimeLessThanEqual(cal.getTime(), 100);
        return candidates.stream().filter(e -> !excludeIds.contains(e.get_id())).toList();
    }

    private void pullMessage(List<Account> accounts, boolean force) {
        for (int i = 0; i < accounts.size(); i++) {
            final Account account = accounts.get(i);
            if (!account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode())) {
                continue;
            }
            try {
                if (shouldPull(account, force)) {
                    if (pullMessageInner(account)) {
                        updateAccountAfterPull(account);
                        if (force) subTaskRepository.updateTimeByAccId(account.get_id());
                    }
                }
            } catch (Exception e) {
                log.error("账号拉取异常: {}", account.getEmail(), e);
            }
        }
    }

    private boolean shouldPull(Account account, boolean force) {
        if (account.getType().equals(AccountTypeEnums.sendgrid.getCode())) {
            return false;
        }
        if (force) return true;
        Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.MINUTE, -(int)(Math.random() * 5) - 3);
        return account.getLastPullMessageTime() == null || cutoff.getTime().after(account.getLastPullMessageTime());
    }

    private void updateAccountAfterPull(Account account) {
        Date now = new Date();
        account.setLastPullMessageTime(now);
        accountRepository.updateLastPullMessageTime(account.get_id(), now);
    }

    private boolean pullMessageInner(Account account) {
        String key = "pullMessageInner:" + account.get_id();
        if (!redisUtil.getLock(key, "1", 60, TimeUnit.SECONDS)) {
            log.info("任务未获取锁，跳过: {}", key);
            return false;
        }

        taskExecutor.execute(() -> {
            try {
                log.info("开始拉取邮件: {}", account.getEmail());

                if (account.getType().equals(AccountTypeEnums.outlook_graph.getCode())) {
                    processOutlook(account);
                } else if (account.getType().equals(AccountTypeEnums.web.getCode())
                        || account.getType().equals(AccountTypeEnums.mobile.getCode())
                        || account.getType().equals(AccountTypeEnums.yahoo.getCode())) {
                    if (StringUtils.isEmpty(account.getSession()) && !initSession(account)) return;

                    int page = 0;
                    while (page < 10) {
                        GetInboxEmailListResponse inbox = gatewayClient.getInboxEmailList(page, account);
                        if (inbox == null || inbox.getCode() != 0 || inbox.getThread_list() == null) break;

                        boolean allExist = processThreads(account, inbox.getThread_list());
                        if (allExist || inbox.getHas_more() == 0) break;
                        page++;
                    }
                } else if (account.getType().equals(AccountTypeEnums.workspace_service_account.getCode())
                        || account.getType().equals(AccountTypeEnums.workspace_second_hand_account.getCode())) {
                    GetInboxEmailListResponse inbox = gatewayClient.getInboxEmailList(1, account);
                    if (inbox != null && inbox.getCode() == 0 && inbox.getThread_list() != null) {
                        processThreads(account, inbox.getThread_list());
                    }
                } else if (account.getType().equals(AccountTypeEnums.smtp.getCode())) {
                    processSmtp(account);
                }
            } catch (Exception e) {
                log.error("拉取邮件失败: {}", account.getEmail(), e);
            } finally {
                log.info("结束拉取邮件: {}", account.getEmail());
                redisUtil.unLock(key);
            }
        });
        return true;
    }

    private void processSmtp(Account account) {
        Message one = messageRepository.findOne(account.get_id(), null);
        List<SmtpMailUtil.MailMessage> emails = null;
        // 正常邮箱
        if (one == null) {
            emails = smtpGatewayClient.getEmails(account, 0);
        } else {
            emails = smtpGatewayClient.getEmails(account, 10);
        }
        if (emails != null &&  !emails.isEmpty()) {
            for (SmtpMailUtil.MailMessage email : emails) {
                saveSmtpEmail(account, email, false);
            }
        }
        if (one == null) {
            emails = smtpGatewayClient.getJunkEmails(account, 0);
        } else {
            emails = smtpGatewayClient.getJunkEmails(account, 10);
        }
        if (emails != null &&  !emails.isEmpty()) {
            for (SmtpMailUtil.MailMessage email : emails) {
                saveSmtpEmail(account, email, true);
            }
        }
    }

    private void saveSmtpEmail(Account account, SmtpMailUtil.MailMessage email, boolean junk) {
        Message message = messageRepository.findOne(account.get_id(), email.getId());
        if (message == null) {
            Message entity = new Message();
            entity.setMessage_id(email.getId());
            entity.setEmail(account.getEmail());
            entity.setAttachments(Map.of("attachments", email.getAttachments()));
            entity.setSubject(email.getSubject());
            entity.setText(Optional.ofNullable(email.getHtmlContent()).orElse(email.getTextContent()));
            entity.setCreateTime(new Date());
            entity.setSummary(email.getTextContent());
            entity.setThread_id(email.getId());
            entity.setAccId(account.get_id());
            entity.setUserID(account.getUserID());
            Date date = email.getSentDate();
            entity.setTimestamp(date.getTime() + "");
            entity.setReceivers(Arrays.stream(email.getTo()).toList());
            entity.setSender(email.getFrom());
            entity.setLabels(junk ? List.of("^all", "^s") : List.of("^all"));

            messageRepository.save(entity);
        }
    }

    private void processOutlook(Account account) {
        Message one = messageRepository.findOne(account.get_id(), null);
        List<OutlookGraphEmail> emails = null;
        if (one == null) {
            emails = outlookGraphGatewayClient.getEmails(account, 0);
        } else {
            emails = outlookGraphGatewayClient.getEmails(account, 10);
        }
        if (emails != null &&  !emails.isEmpty()) {
            List<OutlookGraphFolder> folders = outlookGraphGatewayClient.getFolders(account);
            String junkId = "";
            for (OutlookGraphFolder folder : folders) {
                if (folder.getDisplayName() != null && folder.getDisplayName().equals("Junk Email")) {
                    junkId = folder.getId();
                    break;
                }
            }
            for (OutlookGraphEmail email : emails) {
                saveOutlookEmail(account, email, junkId);
            }
        }

    }

    private void saveOutlookEmail(Account account, OutlookGraphEmail email, String junkId) {
        Message message = messageRepository.findOne(account.get_id(), email.getInternetMessageId());
        if (message == null) {
            Message entity = new Message();
            entity.setMessage_id(email.getInternetMessageId());
            entity.setEmail(account.getEmail());
            entity.setAttachments(Map.of("hasAttachments", email.isHasAttachments()));
            entity.setSubject(email.getSubject());
            entity.setText(Optional.ofNullable(email.getBody().getContent()).orElse(""));
            entity.setCreateTime(new Date());
            entity.setSummary(email.getBodyPreview());
            entity.setThread_id(email.getInternetMessageId());
            entity.setAccId(account.get_id());
            entity.setUserID(account.getUserID());
            Instant instant = Instant.parse(email.getCreatedDateTime());
            Date date = Date.from(instant);
            entity.setTimestamp(date.getTime() + "");
            entity.setReceivers(Optional.of(email.getToRecipients().stream().map(e -> e.getEmailAddress().getAddress()).toList()).orElse(List.of()));
            entity.setSender(Optional.ofNullable(email.getSender().getEmailAddress().getAddress()).orElse(""));
            entity.setLabels(email.getParentFolderId().equals(junkId) ? List.of("^all", "^s") : List.of("^all"));

            messageRepository.save(entity);
        }
    }

    private boolean initSession(Account account) {
        AccountAuthResponse authResp = gatewayClient.accountAuth(account);
        if (authResp != null && authResp.getCode().equals(0)) {
            MakeSessionResponse sessionResp = gatewayClient.makeSession(account);
            return sessionResp.getCode().equals(0);
        }
        return false;
    }

    private boolean processThreads(Account account, List<GetInboxEmailListResponse.Thread> threads) {
        boolean allExist = true;
        for (GetInboxEmailListResponse.Thread thread : threads) {
            List<String> existingIds = messageRepository.findByThreadId(thread.getThread().getThread_id()).stream().map(Message::getMessage_id).toList();
            for (GetInboxEmailListResponse.Message msg : thread.getThread().getMessage_list()) {
                if (existingIds.contains(msg.getMessage_id())) continue;
                allExist = false;
                GetEmailDetailResponse detail = gatewayClient.getEmailDetail(thread.getThread().getThread_id(), List.of(msg.getMessage_id()), account);
                if (detail != null && detail.getCode() == 0 && !detail.getThreads().isEmpty()) {
                    saveMessage(detail.getThreads().getFirst(), account, msg);
                }
            }
        }
        return allExist;
    }

    private void saveMessage(GetEmailDetailResponse.Thread thread, Account account, GetInboxEmailListResponse.Message msg1) {
        GetEmailDetailResponse.Message msg = thread.getMessages().getFirst();
        Message entity = new Message();
        entity.setMessage_id(msg.getMessage_id());
        entity.setEmail(account.getEmail());
        entity.setAttachments(BeanUtil.beanToMap(msg.getMessage_content().getAttachments()));
        entity.setSubject(msg.getMessage_content().getSubject());
        entity.setText(Optional.ofNullable(msg.getMessage_content().getContent_details())
                .map(cd -> cd.getContents() != null ? String.join(" ", cd.getContents().stream().map(e -> e.getBody().getText()).toList()) : "").orElse(""));
        entity.setCreateTime(new Date());
        entity.setSummary(msg.getMessage_content().getSummary());
        entity.setThread_id(thread.getThread_id());
        entity.setAccId(account.get_id());
        entity.setUserID(account.getUserID());
        entity.setTimestamp(msg.getMessage_content().getTimestamp());
        entity.setReceivers(Optional.ofNullable(msg.getMessage_content().getReceivers())
                .map(list -> list.stream().map(Profile::getEmail).toList()).orElse(List.of()));
        entity.setSender(Optional.ofNullable(msg.getMessage_content().getSender()).map(Profile::getEmail).orElse(""));
        entity.setLabels(msg1.getLabels() == null ? new ArrayList<>() : msg1.getLabels());

        messageRepository.save(entity);
    }
}
