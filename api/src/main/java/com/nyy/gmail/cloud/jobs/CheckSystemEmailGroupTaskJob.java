package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.Message;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.MessageRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CheckSystemEmailGroupTaskJob {

    @Autowired
    private GroupTaskRepository  groupTaskRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private AccountRepository accountRepository;

    private volatile boolean is_running = false;

    @Value("${application.taskType}")
    private String taskType;

    @Async("other")
    @Scheduled(cron = "0/2 * * * * ?")
    public void run () {
        if (taskType.equals("googleai")) {
            return;
        }
        if (is_running) return;
        is_running = true;
        try {
            log.info("start CheckSystemEmailGroupTaskJob");
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -10);

            Calendar calendar3 = Calendar.getInstance();
            calendar3.add(Calendar.MINUTE, -10);
            // 检查回复邮件
            int page = 1;
            while (true) {
                PageResult<GroupTask> pagination = groupTaskRepository.findByPagination(100, page, Map.of("type", Map.of("$in", List.of(TaskTypesEnums.BatchSendEmail.getCode(), TaskTypesEnums.EmailLinkCheck.getCode(), TaskTypesEnums.EmailContentAiGen.getCode())), "finishTime", Map.of("$gte", calendar.getTime())));
                if (pagination.getData().isEmpty()) {
                    break;
                }
                for (GroupTask groupTask : pagination.getData()) {
                    List<String> systemEmails = ((List<String>)groupTask.getParams().get("systemEmail"));
                    List<Message> messageList = null;
                    if (systemEmails != null) {
                        Date date = groupTask.getCreateTime();
                        if (date.before(calendar3.getTime())) {
                            date =  calendar3.getTime();
                        }
                        messageList = messageRepository.findByEmailsAndCreateTime(systemEmails, date);
                    }
                    if (messageList != null) {
                        List<String> senders = messageList.stream().map(Message::getSender).toList();
                        List<Account> accountList = accountRepository.findbyEmails(senders);
                        for (Message message : messageList) {
                            List<Account> list = accountList.stream().filter(e -> e.getEmail().equals(message.getSender()) && !message.getAccId().equals(e.get_id())).toList();
                            if (!list.isEmpty()) {
                                subTaskRepository.updateResultLabel(String.join(",", message.getLabels()), list.getFirst().get_id(), message.getEmail(), groupTask.get_id());
                            }
                        }
                    }
                }
                page++;
            }

            // 检查回复邮件
            Calendar calendar2 = Calendar.getInstance();
            calendar2.add(Calendar.HOUR_OF_DAY, -5);
            page = 1;
            while (true) {
                PageResult<GroupTask> pagination = groupTaskRepository.findByPagination(100, page, Map.of("type", Map.of("$in", List.of(TaskTypesEnums.BatchSendEmail.getCode(), TaskTypesEnums.EmailLinkCheck.getCode(), TaskTypesEnums.EmailContentAiGen.getCode())),
                        "status", Map.of("$ne", "success"), "createTime", Map.of("$gte", calendar2.getTime())));
                if (pagination.getData().isEmpty()) {
                    break;
                }
                for (GroupTask groupTask : pagination.getData()) {
                    List<String> systemEmails = ((List<String>)groupTask.getParams().get("systemEmail"));
                    List<Message> messageList = null;
                    if (systemEmails != null) {
                        Date date = groupTask.getCreateTime();
                        if (date.before(calendar3.getTime())) {
                            date =  calendar3.getTime();
                        }
                        messageList = messageRepository.findByEmailsAndCreateTime(systemEmails, date);
                    }
                    if (messageList != null) {
                        List<String> senders = messageList.stream().map(Message::getSender).toList();
                        List<Account> accountList = accountRepository.findbyEmails(senders);
                        for (Message message : messageList) {
                            List<Account> list = accountList.stream().filter(e -> e.getEmail().equals(message.getSender()) && !message.getAccId().equals(e.get_id())).toList();
                            if (!list.isEmpty()) {
                                subTaskRepository.updateResultLabel(String.join(",", message.getLabels()), list.getFirst().get_id(), message.getEmail(), groupTask.get_id());
                            }
                        }
                    }
                }
                page++;
            }
        } finally {
            is_running = false;
        }
    }
}
