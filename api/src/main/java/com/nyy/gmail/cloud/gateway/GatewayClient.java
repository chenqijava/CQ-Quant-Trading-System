package com.nyy.gmail.cloud.gateway;

import com.alibaba.fastjson2.JSON;
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
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.IpUtil;
import com.nyy.gmail.cloud.utils.SendgridUtils;
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
import com.google.gson.reflect.TypeToken;

import java.io.*;
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
public class GatewayClient {

    @Value("${gateway.yahooGatewayBaseUrl}")
    private String yahooGatewayBaseUrl;

    @Value("${gateway.baseUrl}")
    private String baseUrl;

    @Value("${gateway.emailUrl}")
    private String emailUrl;

    @Resource
    private Socks5Service socks5Service;

    @Resource
    private AccountRepository accountRepository;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private ProxyAccountService proxyAccountService;

    @Autowired
    private Socks5Repository socks5Repository;

    private static final List<String> networkErrors = List.of(
            "SSLError(1, '[SSL: WRONG_VERSION_NUMBER] wrong version number (_ssl.c:1129)')",
            "SSLError(1, '[SSL: SSLV3_ALERT_HANDSHAKE_FAILURE] sslv3 alert handshake failure (_ssl.c:1129)')",
            "SSLError",
            "ConnectionResetError",
            "RemoteProtocolError"
    );

    @Autowired
    private CookieGatewayClient cookieGatewayClient;

    @Autowired
    private OutlookGraphGatewayClient outlookGraphGatewayClient;

    @Autowired
    private SmtpGatewayClient smtpGatewayClient;

    private final ConcurrentHashMap<String, AtomicInteger> counter = new ConcurrentHashMap<>();

    public OkHttpClient getHttpClient() {
        return OkHttpClientFactory.getDefaultClient();
    }

    public <T extends GatewayResultBase> T post (Map<String, Object> params, String session, GatewayApiEnums api, Class<T> clazz, Account account) {
        if (api.getCode().equals(GatewayApiEnums.MAKE_SESSION.getCode())) {
            AtomicInteger ai = counter.computeIfAbsent(account.getEmail(), k -> new AtomicInteger(0));
            int current = ai.incrementAndGet(); // 原子递增

            if (current > 10) {
                log.warn("Account {} exceeded MAKE_SESSION retry limit: {}", account.getEmail(), current);
                counter.remove(account.getEmail()); // 清理记录（可选）
                return null;
            }
        }

        OkHttpClient httpClient = getHttpClient();

        // JSON 请求体
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String jsonBody = JSON.toJSONString(params);

        RequestBody body = RequestBody.create(jsonBody, mediaType);

        session = session == null ? "" : session;

        Request request = null;
        Request.Builder builder = new Request.Builder().url((api.getCode().equals(GatewayApiEnums.ACCOUNT_AUTH.getCode()) ? baseUrl : emailUrl)  + api.getCode());
        if (!StringUtils.isEmpty(session)) {
            builder.addHeader("session", session);
        }
        request = builder.post(body).build();

        log.info("api: {} session: {} params: {}", api.getCode(), session.substring(0, Math.min(session.length(), 1000)), jsonBody.substring(0, Math.min(jsonBody.length(), 1000)));

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                String respStr = response.body().string();
                log.info("api: {} session: {} params: {} response:{}", api.getCode(), "", "", respStr.substring(0, Math.min(respStr.length(), 1000)));

                Gson gson = new Gson();

                Type type = TypeToken.getParameterized(clazz).getType();
                T result = gson.fromJson(respStr, type);

                if (result.getCode().equals(-50506) || networkErrors.stream().anyMatch(e -> result.getMsg().contains(e))) {
//                    log.info("api: {} session: {} params: {} error:{}", api.getCode(), "", "", "networkErrors");
                    account.setSocks5Id(null);
                    account.setProxyIp(null);
                    socks5Service.releaseSocks5(account);
                    if (!api.equals(GatewayApiEnums.ACCOUNT_AUTH) && !api.equals(GatewayApiEnums.MAKE_SESSION)) {
                        this.makeSession(account);
                    }
                } else if (!result.getCode().equals(0) && !result.getCode().equals(-50300)) {
//                    log.info("api: {} session: {} params: {} error code:{}", api.getCode(), "", "", result.getCode());
                    account.setSocks5Id(null);
                    account.setProxyIp(null);
                    socks5Service.releaseSocks5(account);
                    if (!api.equals(GatewayApiEnums.ACCOUNT_AUTH) && !api.equals(GatewayApiEnums.MAKE_SESSION)) {
                        this.makeSession(account);
                    }
                }
                if (result.getCode().equals(-50300) && !api.equals(GatewayApiEnums.ACCOUNT_AUTH)) {
//                    log.info("api: {} session: {} params: {} makeSession", api.getCode(), "", "");
                    AccountAuthResponse authResponse = this.accountAuth(account);
                    if (authResponse != null && authResponse.getCode().equals(0)) {
                        this.makeSession(account);
                    }
                }
                return result;
            } else {
                log.error("api: {} session: {} Request failed http status code: {}", api.getCode(), session, response.code());
            }
        } catch (IOException e) {
            log.error("api: {} session: {} Request failed error: {}", api.getCode(), session, e.getMessage());
        } finally {
            if (api.getCode().equals(GatewayApiEnums.MAKE_SESSION.getCode())) {
                AtomicInteger ai = counter.computeIfAbsent(account.getEmail(), k -> new AtomicInteger(0));
                int current = ai.decrementAndGet(); // 原子递增
                if (current <= 0) {
                    counter.remove(account.getEmail()); // 清理记录
                }
            }
        }
        return null;
    }

    public AccountAuthResponse accountAuth (Account account) {
        if (account.getType() != null && account.getType().equals(AccountTypeEnums.web.getCode())) {
            AccountAuthResponse accountAuthResponse = new AccountAuthResponse();
            accountAuthResponse.setCode(0);
            return accountAuthResponse;
        }
        if (account.getType() != null && account.getType().equals(AccountTypeEnums.yahoo.getCode())) {
            return yahooAccountAuth(account);
        }
        AccountAuthResponse result = null;
        for (int i = 0; i < 10; i++) {
            try {
                Map<String, Object> params = new HashMap<>();
                Socks5 socks5 = null;
                if (StringUtils.isNotEmpty(account.getProxyIp()) && !IpUtil.isPrivateIPv4(account.getProxyIp())) {
                    socks5 = new Socks5();
                    socks5.setIp(account.getProxyIp());
                    socks5.setPort(Integer.parseInt(account.getProxyPort()));
                    socks5.setPassword(account.getProxyPassword());
                    socks5.setUsername(account.getProxyUsername());
                } else {
                    socks5 = socks5Service.getAccountSocks(account);
                    if (socks5 == null) {
                        throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                    }
                }


                params.put("proxy", "socks5://" + socks5.getUsername() + ":" + socks5.getPassword() + "@" + socks5.getIp() + ":" + socks5.getPort());
                params.put("email", account.getEmail());
                params.put("app", account.getApp());
                params.put("lstBindingKeyAlias", account.getLstBindingKeyAlias());
                if (StringUtils.isNotEmpty(account.getDeviceToken())) {
                    params.put("token", account.getDeviceToken());
                } else {
                    params.put("googleAccountDataStore", account.getGoogleAccountDataStore().replace("\\n", "\n"));
                }
                if (StringUtils.isNotEmpty(account.getDeviceinfo())) {
                    params.put("deviceinfo", account.getDeviceinfo().replace("\\n", "\n"));
                }
                if (StringUtils.isNotEmpty(account.getDevice())) {
                    params.put("device", account.getDevice().replace("\\n", "\n"));
                }
                result = this.post(params, null, GatewayApiEnums.ACCOUNT_AUTH, AccountAuthResponse.class, account);

//                log.info("accountAuth post end {}, {}", account.getEmail(), JSON.toJSONString(result));
                if (result != null) {
                    if (result.getCode().equals(0) && result.getData() != null && StringUtils.isNotEmpty(result.getData().getIt())) {
                        account.setToken(result.getData().getIt());
                        account.setDeviceinfo(result.getDeviceinfo());
                        accountRepository.updateTokenAndDeviceinfo(account);
                        break;
                    }
                    if (result.getCode().equals(-50000) && result.getMsg().equals("BadAuthentication")) {
                        account.setChangeOnlineStatusTime(new Date());
                        account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
                        account.setLoginError("账号被封：" + result.getMsg());
                        accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
                        break;
                    }
                    if (result.getMsg().equals("DecodeError(\"Error parsing message with type 'GMSKeyPair'\")")) {
                        account.setChangeOnlineStatusTime(new Date());
                        account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
                        account.setLoginError("账号被封：" + result.getMsg());
                        accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
                        break;
                    }
                    if (result.getData() != null && result.getData().getIssueAdvice() != null && result.getData().getIssueAdvice().equals("remote_consent")) {
                        account.setChangeOnlineStatusTime(new Date());
                        account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
                        account.setLoginError("账号被封：remote_consent");
                        accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
                        break;
                    }
                }

                log.info("accountAuth 请求失败 {} {}",account.get_id(), account.getEmail());

                Thread.sleep(1000);
            } catch (CommonException e) {
                if (ResultCode.NO_CAN_USE_SOCKS.getCode().equals(e.getCode())) {
                    account.setChangeOnlineStatusTime(new Date());
                    account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
                    account.setLoginError("账号离线：" + e.getMessage());
                    accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
                }
                throw e;
            } catch (Exception e) {
                log.info("accountAuth ERROR: " + e.getMessage());
            }
        }
        log.info("accountAuth end: {}, {}", account.getEmail(), result == null ? "null" : JSON.toJSONString(result));
        if (result == null || !result.getCode().equals(0)) {
            account.setChangeOnlineStatusTime(new Date());
            account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
            account.setLoginError("账号离线：" + (result == null ? "请求失败2" : result.getMsg()));
            accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
        }
        return result;
    }

    private AccountAuthResponse yahooAccountAuth(Account account) {
        try {
            OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(
                    Map.of("email", account.getEmail(), "cookie", account.getCookie()));
            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request;
            Request.Builder builder = new Request.Builder().url(yahooGatewayBaseUrl + "/api/account/info");
            request = builder.post(body).build();
            try (Response response = defaultClient.newCall(request).execute()) {
                if (response.body() != null && response.code() == 200) {
                    String respStr = response.body().string();

                    JSONObject objRes = JSONObject.parseObject(respStr);

                    Integer code = objRes.getInteger("code");
                    if (code != null && code == 200) {
                        account.setSession(objRes.getString("cookie"));
                        account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                        account.setChangeOnlineStatusTime(new Date());
                        account.setLoginError("");
                        account.setIsCheck(true);
                        accountRepository.updateLoginSuccess(account);
                        AccountAuthResponse accountAuthResponse = new AccountAuthResponse();
                        accountAuthResponse.setCode(0);
                        return accountAuthResponse;
                    } else {
                        if (code == null || code != 500) {
                            account.setChangeOnlineStatusTime(new Date());
                            account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
                            account.setLoginError("账号离线：" + code);
                            accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
                        }
                    }
                } else {
                    if (response.code() != 500) {
                        account.setChangeOnlineStatusTime(new Date());
                        account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
                        account.setLoginError("账号离线：" + response.code());
                        accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
                    }
                }
            }
        } catch (Exception e) {
            log.error("accountAuth 出现异常: {}", account.getEmail(), e);
        }
        AccountAuthResponse accountAuthResponse = new AccountAuthResponse();
        accountAuthResponse.setCode(0);
        return accountAuthResponse;
    }

    public MakeSessionResponse makeSession(Account account) {
        if (account.getType() != null && account.getType().equals(AccountTypeEnums.web.getCode())) {
            return cookieGatewayClient.makeSession(account);
        }
        if (account.getType() != null && account.getType().equals(AccountTypeEnums.yahoo.getCode())) {
            MakeSessionResponse result = new MakeSessionResponse();
            result.setCode(0);
            return result;
        }
        MakeSessionResponse result = null;
        for (int i = 0; i < 10; i++) {
            try {
                Map<String, Object> params = new HashMap<>();
                Socks5 socks5 = null;
                if (StringUtils.isNotEmpty(account.getProxyIp()) && !IpUtil.isPrivateIPv4(account.getProxyIp())) {
                    socks5 = new Socks5();
                    socks5.setIp(account.getProxyIp());
                    socks5.setPort(Integer.parseInt(account.getProxyPort()));
                    socks5.setPassword(account.getProxyPassword());
                    socks5.setUsername(account.getProxyUsername());
                } else {
                    socks5 = socks5Service.getAccountSocks(account);
                    if (socks5 == null) {
                        throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                    }
                }
                if (StringUtils.isEmpty(account.getToken())) {
                    this.accountAuth(account);
                    if (StringUtils.isEmpty(account.getToken())) {
                        return null;
                    }
                }
                params.put("proxy", "socks5://" + socks5.getUsername() + ":" + socks5.getPassword() + "@" + socks5.getIp() + ":" + socks5.getPort());
                params.put("email", account.getEmail());
                params.put("token", account.getToken());
                if (StringUtils.isNotEmpty(account.getDevice())) {
                    params.put("device", account.getDevice().replace("\\n", "\n"));
                }
                if (StringUtils.isNotEmpty(account.getDeviceinfo())) {
                    params.put("deviceinfo", account.getDeviceinfo().replace("\\n", "\n"));
                }
                result = this.post(params, null, GatewayApiEnums.MAKE_SESSION, MakeSessionResponse.class, account);

                if (result != null) {
                    if (result.getCode().equals(0) && StringUtils.isNotEmpty(result.getSession())) {
                        account.setSession(result.getSession());
                        account.setDeviceinfo(result.getDeviceinfo());
                        account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                        account.setChangeOnlineStatusTime(new Date());
                        account.setLoginError("");
                        account.setIsCheck(true);
                        accountRepository.updateLoginSuccess(account);
                        break;
                    }
                }

                log.info("makeSession 请求失败 {} {}",account.get_id(), account.getEmail());

                Thread.sleep(1000);
            } catch (CommonException e) {
                if (ResultCode.NO_CAN_USE_SOCKS.getCode().equals(e.getCode())) {
                    account.setChangeOnlineStatusTime(new Date());
                    account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
                    account.setLoginError("账号离线：" + e.getMessage());
                    accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
                    throw e;
                } else {
                    log.error("makeSession 出现CommonException异常: {}", account.getEmail(), e);
                }
            } catch (Exception e) {
                log.info("makeSession ERROR: " + e.getMessage());
            }
        }
        if (result == null || !result.getCode().equals(0)) {
            account.setChangeOnlineStatusTime(new Date());
            account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
            account.setLoginError("账号离线：" + (result == null ? "请求失败1" : result.getMsg()));
            accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
        }

        return result;
    }


    public GetInboxEmailListResponse getInboxEmailList(Integer page, Account account) {
        if (account.getType() != null && account.getType().equals(AccountTypeEnums.web.getCode())) {
            return cookieGatewayClient.getInboxEmailList(page, account);
        }
        if (account.getType() != null && account.getType().equals(AccountTypeEnums.yahoo.getCode())) {
            return getYahooInboxEmailList(page, account);
        }
        if (account.getType() != null &&
                (
                        account.getType().equals(AccountTypeEnums.workspace_service_account.getCode()) ||
                                account.getType().equals(AccountTypeEnums.workspace_second_hand_account.getCode())
                )) {
            return getWorkspaceEmailList(page, account);
        }
        if (page == null) {
            page = 0;
        }
        GetInboxEmailListResponse result = null;
        for (int i = 0; i < 3; i++) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("page", page);
                result = this.post(params, account.getSession(), GatewayApiEnums.GET_INBOX_EMAIL_LIST, GetInboxEmailListResponse.class, account);
                if (result.getCode().equals(0)) {
                    return result;
                }
            } catch (CommonException e) {
                throw e;
            } catch (Exception e) {
                log.info("getInboxEmailList ERROR: " + e.getMessage());
            }
        }

        return result;
    }

    private GetInboxEmailListResponse getYahooInboxEmailList(Integer page, Account account) {
        try {
            OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(
                    Map.of("email", account.getEmail(), "cookie", account.getCookie(), "session", account.getSession(), "page", page + 1));
            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request;
            Request.Builder builder = new Request.Builder().url(yahooGatewayBaseUrl + "/api/mail/list");
            request = builder.post(body).build();
            try (Response response = defaultClient.newCall(request).execute()) {
                if (response.body() != null && response.code() == 200) {
                    String respStr = response.body().string();

                    JSONObject objRes = JSONObject.parseObject(respStr);

                    Integer code = objRes.getInteger("code");
                    if (code != null && code == 200) {
                        YahooEmailListResponse data = objRes.getObject("data", YahooEmailListResponse.class);
                        Integer total = data.getTotal();
                        List<YahooEmailListResponse.YahooEmailListMessage> messagesResponse = data.getResult();
                        GetInboxEmailListResponse result = new GetInboxEmailListResponse();
                        result.setHas_more(total != null && total < 10 ? 0 : 1);
                        result.setCode(0);
                        Map<String, GetInboxEmailListResponse.ThreadInner> threadInnerMap = new HashMap<>();
                        List<GetInboxEmailListResponse.Thread> threadList = new ArrayList<>();
                        for (YahooEmailListResponse.YahooEmailListMessage messageResponse : messagesResponse) {
                            GetInboxEmailListResponse.ThreadInner threadInner = new GetInboxEmailListResponse.ThreadInner();
                            threadInner.setThread_id(messageResponse.getConversationId());
                            if (threadInnerMap.containsKey(messageResponse.getConversationId())) {
                                threadInner = threadInnerMap.get(messageResponse.getConversationId());
                            } else {
                                threadInnerMap.put(messageResponse.getConversationId(), threadInner);
                            }
                            List<GetInboxEmailListResponse.Message> messages = threadInner.getMessages();
                            if (messages == null) {
                                messages = new ArrayList<>();
                            }
                            GetInboxEmailListResponse.Message message = new GetInboxEmailListResponse.Message();
                            message.setMessage_id(messageResponse.getId());
                            message.setLabels(new ArrayList<>(messageResponse.getFlags().keySet()));
                            if (message.getLabels().contains("spam") || message.getLabels().contains("trash")) {
                                message.getLabels().add("^s");
                            }
                            messages.add(message);
                            threadInner.setMessages(messages);
                        }
                        for (GetInboxEmailListResponse.ThreadInner threadInner : threadInnerMap.values()) {
                            GetInboxEmailListResponse.Thread thread = new GetInboxEmailListResponse.Thread(threadInner);
                            threadList.add(thread);
                        }
                        result.setThread_list(threadList);
                        return result;
                    } else if (code != null) {
                        account.setSession("");
                        accountRepository.updateLoginSuccess(account);
                    }
                }
            }
        } catch (Exception e) {
            log.info("getInboxEmailList ERROR: " + e.getMessage());
        }
        return null;
    }

    private GetInboxEmailListResponse getWorkspaceEmailList(Integer page, Account account) {
        try {
            if (account.getType().equals(AccountTypeEnums.workspace_service_account.getCode())) {
                Gmail service = getServiceAccountGmailService(account.getEmail(), account);
                return getGmailInboxEmailList(service);
            } else if (account.getType().equals(AccountTypeEnums.workspace_second_hand_account.getCode())) {
                Gmail service = getOAuthGmailService(account);
                return getGmailInboxEmailList(service);
            }
        } catch (Exception e) {
            log.info("getInboxEmailList ERROR: " + e.getMessage());
        }
        return null;
    }

    @NotNull
    private GetInboxEmailListResponse getGmailInboxEmailList(Gmail service) throws IOException {
        ListMessagesResponse messagesResponse = service.users().messages().list("me").setIncludeSpamTrash(true).execute();
        GetInboxEmailListResponse result = new GetInboxEmailListResponse();
        result.setHas_more(0);
        result.setCode(0);
        Map<String, GetInboxEmailListResponse.ThreadInner> threadInnerMap = new HashMap<>();
        for (Message messageResponse : messagesResponse.getMessages()) {
            GetInboxEmailListResponse.ThreadInner threadInner = new GetInboxEmailListResponse.ThreadInner();
            threadInner.setThread_id(messageResponse.getThreadId());

            GetInboxEmailListResponse.Message message = new GetInboxEmailListResponse.Message();
            message.setMessage_id(messageResponse.getId());
            message.setLabels(messageResponse.getLabelIds());
            Message me = service.users().messages().get("me", message.getMessage_id()).execute();
            if (me.getLabelIds() == null) {
                message.setLabels(new ArrayList<>());
            } else {
                message.setLabels(me.getLabelIds().stream().map(String::toLowerCase).collect(Collectors.toList()));
                if (message.getLabels().contains("spam") || message.getLabels().contains("trash")) {
                    message.getLabels().add("^s");
                }
            }
            if (me.getLabelIds() != null && !me.getLabelIds().contains("SENT")) {
                if (threadInnerMap.containsKey(messageResponse.getThreadId())) {
                    threadInner = threadInnerMap.get(messageResponse.getThreadId());
                } else {
                    threadInnerMap.put(messageResponse.getThreadId(), threadInner);
                }
                List<GetInboxEmailListResponse.Message> messages = threadInner.getMessages();
                if (messages == null) {
                    messages = new ArrayList<>();
                }
                messages.add(message);
                threadInner.setMessages(messages);
            }
        }
        List<GetInboxEmailListResponse.Thread> threadList = new ArrayList<>();
        for (GetInboxEmailListResponse.ThreadInner threadInner : threadInnerMap.values()) {
            GetInboxEmailListResponse.Thread thread = new GetInboxEmailListResponse.Thread(threadInner);
            threadList.add(thread);
        }
        result.setThread_list(threadList);
        return result;
    }

    private GetEmailDetailResponse getWorkspaceEmailDetail(String threadId, List<String> messageIds, Account account) {
        try {
            if (account.getType().equals(AccountTypeEnums.workspace_service_account.getCode())) {
                Gmail service = getServiceAccountGmailService(account.getEmail(), account);
                return getGmailEmailDetail(service, threadId, messageIds);
            } else if (account.getType().equals(AccountTypeEnums.workspace_second_hand_account.getCode())) {
                Gmail service = getOAuthGmailService(account);
                return getGmailEmailDetail(service, threadId, messageIds);
            }
        } catch (Exception e) {
            log.info("getInboxEmailList ERROR: " + e.getMessage());
        }
        return null;
    }

    private GetEmailDetailResponse getGmailEmailDetail(Gmail service, String threadId, List<String> messageIds) throws IOException {
        GetEmailDetailResponse result = new GetEmailDetailResponse();
        result.setCode(0);
        List<GetEmailDetailResponse.Message> messages = new ArrayList<>();
        for (String messageId : messageIds) {
            Message msg = service.users().messages().get("me", messageId).execute();
            GetEmailDetailResponse.Message message = new GetEmailDetailResponse.Message();
            message.setMessage_id(messageId);
            GetEmailDetailResponse.MessageContent messageContent = new GetEmailDetailResponse.MessageContent();
            if (msg.getPayload().getHeaders() != null && msg.getPayload().getHeaders().stream().anyMatch(messagePartHeader ->
                    messagePartHeader.getName().equalsIgnoreCase("Subject"))) {
                String subject = msg.getPayload().getHeaders().stream().filter(messagePartHeader ->
                        messagePartHeader.getName().equalsIgnoreCase("Subject")).findFirst().get().getValue();
                messageContent.setSubject(subject);
            }
            if (msg.getPayload().getHeaders() != null && msg.getPayload().getHeaders().stream().anyMatch(messagePartHeader ->
                    messagePartHeader.getName().equalsIgnoreCase("Date"))) {
                String date = msg.getPayload().getHeaders().stream().filter(messagePartHeader ->
                        messagePartHeader.getName().equalsIgnoreCase("Date")).findFirst().get().getValue();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z (zzz)");
                String timestamp = "";
                try {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(date, formatter.withLocale(java.util.Locale.ENGLISH));
                    timestamp = String.valueOf(zonedDateTime.toInstant().toEpochMilli());
                } catch (DateTimeParseException e) {
                }
                if (StringUtils.isBlank(timestamp)) {
                    try {
                        ZonedDateTime zonedDateTime = ZonedDateTime.parse(date);
                        timestamp = String.valueOf(zonedDateTime.toInstant().toEpochMilli());
                    } catch (DateTimeParseException e) {
                    }
                }
                messageContent.setTimestamp(timestamp);
            }
            if (msg.getPayload().getHeaders() != null && msg.getPayload().getHeaders().stream().anyMatch(messagePartHeader ->
                    messagePartHeader.getName().equalsIgnoreCase("From"))) {
                Profile sender = new Profile();
                String from = msg.getPayload().getHeaders().stream().filter(messagePartHeader ->
                        messagePartHeader.getName().equalsIgnoreCase("From")).findFirst().get().getValue();
                if (StringUtils.isNotBlank(from)) {
                    sender.setName(from);
                    sender.setEmail(extractSingleEmail(from));
                }
                messageContent.setSender(sender);
            }
            if (msg.getPayload().getHeaders() != null && msg.getPayload().getHeaders().stream().anyMatch(messagePartHeader ->
                    messagePartHeader.getName().equalsIgnoreCase("To"))) {
                List<Profile> receivers = new ArrayList<>();
                String tos = msg.getPayload().getHeaders().stream().filter(messagePartHeader ->
                        messagePartHeader.getName().equalsIgnoreCase("To")).findFirst().get().getValue();
                if (StringUtils.isNotBlank(tos)) {
                    for (String to : tos.split(",")) {
                        Profile receiver = new Profile();
                        receiver.setName(to);
                        receiver.setEmail(extractSingleEmail(to));
                        receivers.add(receiver);
                    }
                }
                messageContent.setReceivers(receivers);
            }
            messageContent.setSummary(msg.getSnippet());
            GetEmailDetailResponse.ContentDetail contentDetail = new GetEmailDetailResponse.ContentDetail();
            contentDetail.setContents(List.of(new GetEmailDetailResponse.ContentsInner(
                    new GetEmailDetailResponse.ContentBody(extractBodyFromPart(msg.getPayload())))));
            messageContent.setContent_details(contentDetail);
            message.setMessage_content(messageContent);
            messages.add(message);
        }
        GetEmailDetailResponse.Thread thread = new GetEmailDetailResponse.Thread();
        result.setThreads(Collections.singletonList(thread));
        thread.setThread_id(threadId);
        thread.setMessages(messages);
        return result;
    }

    private String extractBodyFromPart(MessagePart part) {
//        // 1. 单部分内容（直接解码）
//        if (part.getParts() == null || part.getParts().isEmpty()) {
//            // 判断是否为纯文本或 HTML 正文
//            String mimeType = part.getMimeType();
//            if (mimeType.equals("text/plain") || mimeType.equals("text/html")) {
//                return decodeBase64(part.getBody().getData());
//            }
//            return ""; // 非正文部分（如附件）忽略
//        }
//
//        // 2. 多部分内容（递归解析子部分）
//        StringBuilder body = new StringBuilder();
//        List<MessagePart> subParts = part.getParts();
//        for (MessagePart subPart : subParts) {
//            String subBody = extractBodyFromPart(subPart);
//            if (!subBody.isEmpty()) {
//                body.append(subBody).append("\n");
//            }
//        }
//        return body.toString().trim();
        if (part == null) return "";
        List<MessagePart> subParts = part.getParts();
        if (subParts == null || subParts.isEmpty()) {
            String mimeType = part.getMimeType();
            if (StringUtils.equalsIgnoreCase(mimeType, "text/plain") || StringUtils.equalsIgnoreCase(mimeType, "text/html")) {
                MessagePartBody body = part.getBody();
                if (body == null) return "";
                return decodeBase64(body.getData());
            }
            return "";
        }
        StringBuilder bodyBuilder = new StringBuilder();
        for (MessagePart subPart : subParts) {
            String subBody = extractBodyFromPart(subPart);
            if (StringUtils.isNotBlank(subBody)) {
                bodyBuilder.append(subBody).append("\n");
            }
        }
        return bodyBuilder.toString().trim();
    }

    // Base64 解码（Gmail 使用的是 URL 安全的 Base64 编码）
    private String decodeBase64(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            return "";
        }
        // 替换 URL 安全 Base64 的字符（'-'→'+', '_'→'/'），并补全 padding
        String normalizedData = base64Data.replace('-', '+').replace('_', '/');
        int padding = (4 - normalizedData.length() % 4) % 4;
        for (int i = 0; i < padding; i++) {
            normalizedData += "=";
        }
        // 解码为 UTF-8 字符串
        byte[] decodedBytes = Base64.decodeBase64(normalizedData);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private String extractSingleEmail(String input) {
//        if (input == null || input.trim().isEmpty()) {
//            return null;
//        }
//        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{2,})");
//
//        Matcher matcher = emailPattern.matcher(input);
//        // 找到第一个（也是唯一的）合法邮箱（默认字符串中只有一个邮箱）
//        if (matcher.find()) {
//            return matcher.group();
//        }
//        return null;
        if (StringUtils.isBlank(input)) return null;
        try {
            InternetAddress[] addresses = InternetAddress.parse(input, false);
            if (addresses != null && addresses.length > 0) {
                return addresses[0].getAddress();
            }
        } catch (AddressException e) {
            log.debug("Failed to parse email from '{}': {}", input, e.getMessage());
        }
        // fallback to regex if you want
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9+._%\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = emailPattern.matcher(input);
        if (matcher.find()) return matcher.group();
        return null;
    }

    public GetEmailDetailResponse getEmailDetail(String threadId, List<String> messageIds, Account account) {
        if (account.getType() != null && account.getType().equals(AccountTypeEnums.web.getCode())) {
            GetEmailDetailResponse emailDetail = cookieGatewayClient.getEmailDetail(threadId, messageIds, account);
            return emailDetail;
        }
        if (account.getType() != null && account.getType().equals(AccountTypeEnums.yahoo.getCode())) {
            GetEmailDetailResponse emailDetail = getYahooEmailDetail(threadId, messageIds, account);
            return emailDetail;
        }
        if (account.getType() != null &&
                (
                        account.getType().equals(AccountTypeEnums.workspace_service_account.getCode()) ||
                                account.getType().equals(AccountTypeEnums.workspace_second_hand_account.getCode())
                )) {
            return getWorkspaceEmailDetail(threadId, messageIds, account);
        }
        GetEmailDetailResponse result = null;
        for (int i = 0; i < 3; i++) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("thread_id", threadId);
                if (messageIds != null && messageIds.size() > 0) {
                    params.put("message_ids", messageIds);
                }
                result = this.post(Map.of("threads", List.of(params)), account.getSession(), GatewayApiEnums.GET_EMAIL_DETAIL, GetEmailDetailResponse.class, account);
                if (result.getCode().equals(0)) {
                    return result;
                }

            } catch (CommonException e) {
                throw e;
            } catch (Exception e) {
                log.info("getEmailDetail ERROR: " + e.getMessage());
            }
        }

        return result;
    }

    private GetEmailDetailResponse getYahooEmailDetail(String threadId, List<String> messageIds, Account account) {
        try {
            GetEmailDetailResponse result = new GetEmailDetailResponse();
            result.setCode(0);
            List<GetEmailDetailResponse.Message> messages = new ArrayList<>();
            for (String messageId : messageIds) {
                OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
                MediaType mediaType = MediaType.parse("application/json");
                String jsonBody = JSON.toJSONString(
                        Map.of("email", account.getEmail(), "cookie", account.getCookie(), "session", account.getSession(), "mail_id", messageId));
                RequestBody body = RequestBody.create(jsonBody, mediaType);

                Request request;
                Request.Builder builder = new Request.Builder().url(yahooGatewayBaseUrl + "/api/mail/detail");
                request = builder.post(body).build();
                try (Response response = defaultClient.newCall(request).execute()) {
                    if (response.body() != null && response.code() == 200) {
                        String respStr = response.body().string();

                        JSONObject objRes = JSONObject.parseObject(respStr);

                        Integer code = objRes.getInteger("code");
                        if (code != null && code == 200) {
                            YahooEmailDetailResponse data = objRes.getObject("data", YahooEmailDetailResponse.class);
                            GetEmailDetailResponse.Message message = new GetEmailDetailResponse.Message();
                            message.setMessage_id(messageId);
                            GetEmailDetailResponse.MessageContent messageContent = new GetEmailDetailResponse.MessageContent();
                            messageContent.setSubject(data.getMessage().getHeaders().getSubject());
                            messageContent.setTimestamp(String.valueOf(data.getMessage().getHeaders().getDate() * 1000));
                            messageContent.setSender(data.getMessage().getHeaders().getFrom().getFirst());
                            messageContent.setReceivers(data.getMessage().getHeaders().getTo());
                            messageContent.setSummary(data.getMessage().getSnippet());
                            GetEmailDetailResponse.ContentDetail contentDetail = new GetEmailDetailResponse.ContentDetail();
                            contentDetail.setContents(List.of(new GetEmailDetailResponse.ContentsInner(
                                    new GetEmailDetailResponse.ContentBody(data.getSimpleBody().getHtml()))));
                            messageContent.setContent_details(contentDetail);
                            message.setMessage_content(messageContent);
                            messages.add(message);
                            GetEmailDetailResponse.Thread thread = new GetEmailDetailResponse.Thread();
                            result.setThreads(Collections.singletonList(thread));
                            thread.setThread_id(threadId);
                            thread.setMessages(messages);
                            return result;
                        } else if (code != null) {
                            account.setSession("");
                            accountRepository.updateLoginSuccess(account);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("getInboxEmailList ERROR: " + e.getMessage());
        }
        return null;
    }

    private SendEmailResponse _sendEmail(String subject, String sender_name, List<String> receivers, String content, Account account) {
        SendEmailResponse result = null;
        for (int i = 0; i < 3; i++) {
            try {
                if (account.getType() == null || account.getType().equals(AccountTypeEnums.mobile.getCode())) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("subject", subject);
                    params.put("sender_name", sender_name);
                    params.put("receivers", receivers);
                    params.put("content", content);
                    result = this.post(params, account.getSession(), GatewayApiEnums.SEND_EMAIL, SendEmailResponse.class, account);
                    if (result.getCode().equals(0)) {
                        return result;
                    }
                } else if (account.getType().equals(AccountTypeEnums.sendgrid.getCode())) {
                    Socks5 socks5 = null;
                    if (StringUtils.isEmpty(account.getSocks5Id())) {
                        socks5 = socks5Service.getAccountSocks(account);
                        if (socks5 == null) {
                            throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                        }
                        account.setSocks5Id(socks5.get_id());

                        accountRepository.updateSocks5Id(account.get_id(), account.getSocks5Id());
                    } else {
                        socks5 = socks5Repository.findSocks5ById(account.getSocks5Id());
                        if (socks5 == null) {
                            socks5 = socks5Service.getAccountSocks(account);
                            if (socks5 == null) {
                                throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                            }
                            account.setSocks5Id(socks5.get_id());

                            accountRepository.updateSocks5Id(account.get_id(), account.getSocks5Id());
                        }
                    }
                    if (socks5 == null) {
                        throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                    }
                    try {
                        String s = SendgridUtils.sendEmail(socks5, sender_name, account.getSendGridApiKey(), receivers, subject, content);
                        SendEmailResponse sendEmailResponse = new SendEmailResponse();
                        sendEmailResponse.setCode(0);
                        sendEmailResponse.setMsg(s);
                        return sendEmailResponse;
                    } catch (CommonException e) {
                        if (e.getCode() == ResultCode.INSUFFICIENT_USER_BALANCE.getCode()) {
                            accountRepository.updateOnlineStatus(account.get_id(), AccountOnlineStatus.OFFLINE.getCode(), "额度已用完");
                        }
                        throw e;
                    } catch (Exception e) {
                        log.info("SendgridUtils sendEmail ERROR: " + e.getMessage());
                        socks5Service.releaseSocks5(account.get_id(), account.getSocks5Id(), null);
                        account.setSocks5Id("");
                    }
                } else if (account.getType().equals(AccountTypeEnums.web.getCode())) {
                    return cookieGatewayClient.sendEmail(subject, sender_name, receivers, content, account);
                } else if (account.getType().equals(AccountTypeEnums.outlook_graph.getCode())) {
                    SendEmailResponse sendEmailResponse = new SendEmailResponse();
                    boolean response = outlookGraphGatewayClient.sendEmail(subject, receivers, content, account);
                    if (response) {
                        sendEmailResponse.setCode(0);
                        sendEmailResponse.setMsg("");
                    } else {
                        throw new CommonException(ResultCode.SEND_MAIL_FAIL);
                    }
                    return sendEmailResponse;
                } else if (account.getType().equals(AccountTypeEnums.workspace_service_account.getCode())) {
                    Gmail service = getServiceAccountGmailService(sender_name, account);

                    // Encode as MIME message
                    return sendGmail(subject, sender_name, receivers, content, service);
                } else if (account.getType().equals(AccountTypeEnums.workspace_second_hand_account.getCode())) {
                    Gmail service = getOAuthGmailService(account);

                    // Encode as MIME message
                    return sendGmail(subject, sender_name, receivers, content, service);
                } else if (account.getType().equals(AccountTypeEnums.yahoo.getCode())) {
                    return sendYahooEmail(account, subject, sender_name, receivers, content);
                } else if (account.getType().equals(AccountTypeEnums.smtp.getCode())) {
                    SendEmailResponse sendEmailResponse = new SendEmailResponse();
                    boolean response = smtpGatewayClient.sendEmail(subject, receivers, content, account);
                    if (response) {
                        sendEmailResponse.setCode(0);
                        sendEmailResponse.setMsg("");
                    } else {
                        throw new CommonException(ResultCode.SEND_MAIL_FAIL);
                    }
                    return sendEmailResponse;
                }
            } catch (CommonException e) {
                throw e;
            } catch (Exception e) {
                log.info("sendEmail ERROR: " + e.getMessage());
            }
        }

        return result;
    }

    private SendEmailResponse sendYahooEmail(Account account, String subject, String senderName, List<String> receivers, String content) {
        try {
                OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
                MediaType mediaType = MediaType.parse("application/json");
                String jsonBody = JSON.toJSONString(
                        Map.of("email", account.getEmail(), "cookie", account.getCookie(), "session", account.getSession(),
                                "to", String.join(",", receivers),
                                "subject", subject, "content", content
                        ));
                RequestBody body = RequestBody.create(jsonBody, mediaType);

                Request request;
                Request.Builder builder = new Request.Builder().url(yahooGatewayBaseUrl + "/api/mail/send");
                request = builder.post(body).build();
                try (Response response = defaultClient.newCall(request).execute()) {
                    if (response.body() != null && response.code() == 200) {
                        String respStr = response.body().string();
                        JSONObject objRes = JSONObject.parseObject(respStr);
                        SendEmailResponse sendEmailResponse = new SendEmailResponse();
                        sendEmailResponse.setCode(0);
                        sendEmailResponse.setMsg(objRes.getString("message"));
                        return sendEmailResponse;
                    }
                }
        } catch (Exception e) {
            log.info("sendEmail ERROR: " + e.getMessage());
        }
        throw new CommonException(ResultCode.SEND_MAIL_FAIL);
    }

    @NotNull
    private static SendEmailResponse sendGmail(String subject, String sender_name, List<String> receivers, String content, Gmail service) throws MessagingException, IOException, MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(sender_name));
        for (String receiver : receivers) {
            email.addRecipient(jakarta.mail.Message.RecipientType.TO,
                    new InternetAddress(receiver));
        }
        email.setSubject(subject);
        email.setText(content);

        // Encode and wrap the MIME message into a gmail message
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
        Message message = new Message();
        message.setRaw(encodedEmail);

        try {
            // Create send message
            message = service.users().messages().send("me", message).execute();
            SendEmailResponse sendEmailResponse = new SendEmailResponse();
            sendEmailResponse.setCode(0);
            sendEmailResponse.setMsg(message.toPrettyString());
            return sendEmailResponse;
        } catch (GoogleJsonResponseException e) {
            throw new CommonException(ResultCode.SEND_MAIL_FAIL);
        }
    }

    @NotNull
    private static Gmail getOAuthGmailService(Account account) {
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(UserCredentials.newBuilder()
                .setClientId(account.getWorkspaceClientId())
                .setClientSecret(account.getWorkspaceClientSecret())
                .setRefreshToken(account.getWorkspaceRefreshToken())
                .build());
        // Create the gmail API client
        Gmail service = new Gmail.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Gmail")
                .build();
        return service;
    }

    @NotNull
    private static Gmail getServiceAccountGmailService(String sender_name, Account account) throws IOException {
//        Path resPath = FileUtils.resPath;
//        Path emailFileResPath = resPath.resolve(account.getWorkspaceCredentialJSON()).toAbsolutePath().normalize();
//        InputStream inputStream = new FileInputStream(emailFileResPath.toString());
//        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(inputStream).createScoped(GmailScopes.GMAIL_SEND, GmailScopes.GMAIL_READONLY).createDelegated(sender_name);
//        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
//
//        // Create the gmail API client
//        Gmail service = new Gmail.Builder(new NetHttpTransport(),
//                GsonFactory.getDefaultInstance(),
//                requestInitializer)
//                .setApplicationName("Gmail")
//                .build();
//        return service;
        Path resPath = FileUtils.resPath;
        Path emailFileResPath = resPath.resolve(account.getWorkspaceCredentialJSON()).toAbsolutePath().normalize();
        try (InputStream inputStream = new FileInputStream(emailFileResPath.toString())) {
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(inputStream)
                    .createScoped(GmailScopes.GMAIL_SEND, GmailScopes.GMAIL_READONLY)
                    .createDelegated(sender_name);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
            return new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), requestInitializer)
                    .setApplicationName("Gmail")
                    .build();
        }
    }

    public SendEmailResponse sendEmail(String subject, String sender_name, List<String> receivers, String content, Account account) {
        SendEmailResponse response = _sendEmail(subject, sender_name, receivers, content, account);
        if (response != null && response.getCode().equals(0)) {
            for (int i = 0; i < 10; i++) {
                try {
                    Account account1 = accountRepository.findById(account.get_id());
                    account1.setSendEmailTotal(account1.getSendEmailTotal() + 1);
                    accountRepository.update(account1);
                    break;
                } catch (Exception e) {
                }
            }
        }
        return response;
    }
}
