package com.nyy.gmail.cloud.gateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.AccountTypeEnums;
import com.nyy.gmail.cloud.enums.GatewayApiEnums;
import com.nyy.gmail.cloud.gateway.dto.*;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.Socks5Repository;
import com.nyy.gmail.cloud.service.ProxyAccountService;
import com.nyy.gmail.cloud.service.Socks5Service;
import com.nyy.gmail.cloud.utils.*;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SmtpGatewayClient {

    @Autowired
    private AccountRepository accountRepository;

    public List<SmtpMailUtil.MailMessage> getEmails (Account account, int top) {
        String error = "";
        for (int i = 0; i < 3; i++) {
            try {
                List<SmtpMailUtil.MailMessage> mailMessages = SmtpMailUtil.receiveInboxMail(
                        account.getImapHost(), Integer.parseInt(account.getImapPort()), account.getSmtpUsername(), account.getSmtpPassword(), account.isImapSsl(), top);
                if (!account.getIsCheck()) {
                    account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                    account.setChangeOnlineStatusTime(new Date());
                    account.setLoginError("");
                    account.setIsCheck(true);
                    accountRepository.updateLoginSuccess(account);
                }
                return mailMessages;
            } catch (CommonException e) {
                return new ArrayList<>();
            } catch (Exception e) {
                log.error("_id {} api: getEmails failed error: {}", account.get_id(), e.getMessage());
                error = e.getMessage();
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
        }
        // 失败
        account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
        account.setChangeOnlineStatusTime(new Date());
        account.setLoginError("账号离线：" + error);
        account.setIsCheck(true);
        accountRepository.updateLoginSuccess(account);
        return new ArrayList<>();
    }

    public List<SmtpMailUtil.MailMessage> getJunkEmails (Account account, int top) {
        for (int i = 0; i < 3; i++) {
            try {
                return SmtpMailUtil.receiveSpamMail(
                        account.getImapHost(), Integer.parseInt(account.getImapPort()), account.getSmtpUsername(), account.getSmtpPassword(), account.isImapSsl(), top);
            } catch (CommonException e) {
                return new ArrayList<>();
            } catch (Exception e) {
                log.error("_id {} api: getEmails failed error: {}", account.get_id(), e.getMessage());
            }
        }

        return new ArrayList<>();
    }

    public boolean sendEmail (String subject, List<String> receivers, String content, Account account) {
        String error = "";
        for (int i = 0; i < 3; i++) {
            try {
                SmtpMailUtil.sendMail(
                        account.getSmtpHost(), Integer.parseInt(account.getSmtpPort()), account.getSmtpUsername(), account.getSmtpPassword(),
                        account.getEmail(), receivers, subject, content, true, null);
                return true;
            } catch (Exception e) {
                log.error("_id {} api: sendEmail failed error: {}", account.get_id(), e.getMessage());
                error = e.getMessage();
            }
        }
        throw new CommonException(ResultCode.SEND_MAIL_FAIL, error);
    }

}
