package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.BuyEmailDetailOrderStatus;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GetCodeService {

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private AccountPlatformRepository accountPlatformRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EmailReceiveRecordRepository emailReceiveRecordRepository;

    @Autowired
    private BuyEmailOrderDetailRepository buyEmailOrderDetailRepository;

    public static String filterHtmlUsingRegex(String htmlInput) {
        // 使用正则表达式过滤HTML标签和特殊字符
        String filteredHtml = htmlInput.replaceAll("<[^>]*>", "")
                .replaceAll("&[a-zA-Z]*;", "")
                .replaceAll("\\s+", " ");
        return filteredHtml;
    }

    public Map<String, String> getCode(String subTaskId, String accId) {
        Map<String, String> result = new HashMap<>();
        result.put("curTime", DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        SubTask subTask = subTaskRepository.findById(subTaskId);
        if (subTask == null || !subTask.getAccid().equals(accId)) {
            result.put("error", "The link has expired");
            return result;
        }
        Account account = accountRepository.findById(accId);
        if (account == null) {
            result.put("error", "The link has expired");
            return result;
        }
        result.put("email", account.getEmail());

        if (!account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode())) {
            result.put("error", "The account has been banned");
            return result;
        }

        String platformId = subTask.getParams().get("platformId").toString();
        AccountPlatform platform = accountPlatformRepository.findOneByIdAndUserID(platformId, subTask.getUserID());
        if (platform == null) {
            result.put("error", "The link has expired");
            return result;
        }
        List<Message> messageList = messageRepository.findByAccIdAndCreateTimeGatherThan(accId, subTask.getCreateTime());
        messageList = messageList.stream().filter(e -> {
            if (StringUtils.isEmpty(platform.getEmailFrom())) {
                return true;
            }
            String[] split = platform.getEmailFrom().split(",", -1);
            for (String p : split) {
                if (e.getSender().contains(p)) {
                    return true;
                }
            }
            return false;
        }).sorted((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime())).toList();

        if (!messageList.isEmpty()) {
            // 如果不存在就说明已经获取了
            BuyEmailOrderDetail detail = buyEmailOrderDetailRepository.findByEmailAndStatusAndPlatformId(account.getEmail(), BuyEmailDetailOrderStatus.Waiting.getCode(), platformId);
            if (detail != null) {
                return result;
            }

            Message first = messageList.getFirst();
            result.put("time", DateUtil.formatByDate(StringUtils.isEmpty(first.getTimestamp()) ? new Date() : new Date(Long.parseLong(first.getTimestamp())), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            if (StringUtils.isNotEmpty(platform.getPattern())) {
                Pattern pattern = Pattern.compile(platform.getPattern());
                Matcher matcher = pattern.matcher(platform.getPattern().contains("http") ? first.getText() : filterHtmlUsingRegex(first.getText()));
                if (matcher.find()) {
                    if (matcher.groupCount() > 0) {
                        result.put("text", matcher.group(1));
                    } else {
                        result.put("text", matcher.group());
                    }
                }
            } else {
                String text = filterHtmlUsingRegex(first.getText());
                result.put("text", text);
            }

            // 修改ACCOUNT
            account = accountRepository.findById(accId);
            if (account.getRealUsedPlatformIds() == null) {
                account.setRealUsedPlatformIds(new ArrayList<>());
            }
            if (!account.getRealUsedPlatformIds().contains(platformId)) {
                account.getRealUsedPlatformIds().add(platformId);
                accountRepository.update(account);
            }
            // 修改SUBTASK
            if (StringUtils.isEmpty(subTask.getTmpId()) || subTask.getTmpId().equals("2")) {
                subTask = subTaskRepository.findById(subTaskId);
                subTask.setTmpId("1");
                if (subTask.getParams() != null && !subTask.getParams().containsKey("realUsedTime")) {
                    subTask.getParams().put("realUsedTime", new Date());
                }
                subTaskRepository.save(subTask);
            } else if (subTask.getTmpId().equals("1")) {
                subTask = subTaskRepository.findById(subTaskId);
                subTask.setTmpId("2");
                subTaskRepository.save(subTask);
            }

            // 保存记录
            EmailReceiveRecord record = emailReceiveRecordRepository.findOneBySubTaskIdAndAccId(subTaskId, accId);
            if (record == null) {
                record = new EmailReceiveRecord();
                record.setEmail(account.getEmail());
                record.setText(result.get("text"));
                record.setCreateTime(new Date());
                record.setAllText(first.getText());
                record.setPlatformId(platform.get_id());
                record.setPlatformName(platform.getName());
                record.setAccId(account.get_id());
                record.setUserID(account.getUserID());
                record.setReceiveTime(StringUtils.isEmpty(first.getTimestamp()) ? new Date() : new Date(Long.parseLong(first.getTimestamp())));
                record.setSubTaskId(subTask.get_id());
                emailReceiveRecordRepository.save(record);
            }
            return result;
        }

        if (subTask.getTmpId() == null) {
            subTask = subTaskRepository.findById(subTaskId);
            subTask.setTmpId("2");
            subTaskRepository.save(subTask);
        }

        return result;
    }
}
