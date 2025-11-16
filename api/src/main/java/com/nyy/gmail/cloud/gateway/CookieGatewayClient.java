package com.nyy.gmail.cloud.gateway;

import com.alibaba.fastjson2.JSON;
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
import com.nyy.gmail.cloud.utils.IpUtil;
import com.nyy.gmail.cloud.utils.UUIDUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CookieGatewayClient {

    @Value("${gateway.gmailCookieBaseUrl}")
    private String baseUrl;

    @Resource
    private Socks5Service socks5Service;

    @Resource
    private AccountRepository accountRepository;

    @Autowired
    private ProxyAccountService proxyAccountService;

    @Autowired
    private Socks5Repository socks5Repository;

    @Autowired
    private RedissonClient redissonClient;

    private static final List<String> networkErrors = List.of(
            "SSLError(1, '[SSL: WRONG_VERSION_NUMBER] wrong version number (_ssl.c:1129)')",
            "SSLError(1, '[SSL: SSLV3_ALERT_HANDSHAKE_FAILURE] sslv3 alert handshake failure (_ssl.c:1129)')",
            "SSLError",
            "ConnectionResetError",
            "RemoteProtocolError",
            "400:['er'"
    );

    public OkHttpClient getHttpClient() {
        return OkHttpClientFactory.getDefaultClient();
    }

    public <T extends GatewayResultBase> T post (Map<String, Object> params, String session, GatewayApiEnums api, Class<T> clazz, Account account) {
        RBucket<String> bucket = redissonClient.getBucket("CookieGatewayClientLock");
        if (bucket.isExists()) {
            return null;
        }

        OkHttpClient httpClient = getHttpClient();

        // JSON 请求体
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String jsonBody = JSON.toJSONString(params);

        RequestBody body = RequestBody.create(jsonBody, mediaType);

        session = session == null ? "" : session;

        Request request = null;
        Request.Builder builder = new Request.Builder().url((baseUrl)  + api.getCode());
        if (!StringUtils.isEmpty(session)) {
            builder.addHeader("session", session);
        }
        request = builder.post(body).build();

        log.info("api: {} session: {} params: {}", api.getCode(), session.substring(0, Math.min(session.length(), 1000)), jsonBody.substring(0, Math.min(jsonBody.length(), 1000)));

        try (Response response = httpClient.newCall(request).execute()) {
            String newSession = response.header("session");
            if (StringUtils.isNotEmpty(newSession)) {
                accountRepository.updateSession(account.get_id(), newSession);
            }
            if (response.code() == 502) {
                bucket = redissonClient.getBucket("CookieGatewayClientLock");
                bucket.set("1", Duration.ofSeconds(10));
            }

            if (response.body() != null) {
                String respStr = response.body().string();
                log.info("api: {} session: {} params: {} response:{}", api.getCode(), "", "", respStr.substring(0, Math.min(respStr.length(), 1000)));

                List<String> unauthList = List.of("401:['er", "status:401, reason:Unauthorized");
                if (unauthList.stream().anyMatch(respStr::contains)) {
                    this.makeSession(account);
                    return null;
                }

                if (clazz == null) {
                    OtherGatewayResultBase otherGatewayResultBase = new OtherGatewayResultBase();
                    otherGatewayResultBase.setAllResponseStr(respStr);
                    return (T) otherGatewayResultBase;
                }
                Gson gson = new Gson();

                Type type = TypeToken.getParameterized(clazz).getType();
                T result = gson.fromJson(respStr, type);

                if (response.code() != 200 && (result.getCode().equals(-50506) || networkErrors.stream().anyMatch(e -> result.getMsg().contains(e)))) {
                    account.setSocks5Id(null);
                    account.setProxyIp(null);
                    socks5Service.releaseSocks5(account);
                    this.makeSession(account);
                }

                return result;
            } else {
                log.error("api: {} session: {} Request failed http status code: {}", api.getCode(), session, response.code());
            }
        } catch (IOException e) {
            if (e.getMessage().contains("timeout")) {
                account.setSocks5Id(null);
                account.setProxyIp(null);
                socks5Service.releaseSocks5(account);
                this.makeSession(account);
            }
            log.error("api: {} session: {} Request failed error: {}", api.getCode(), session, e.getMessage());
        }
        return null;
    }

    public MakeSessionResponse makeSession(Account account) {
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
                if (StringUtils.isEmpty(account.getCookie())) {
                    throw new CommonException(ResultCode.ACCOUNT_NO_TOKEN);
                }
                params.put("proxy", "socks5://" + socks5.getUsername() + ":" + socks5.getPassword() + "@" + socks5.getIp() + ":" + socks5.getPort());
                params.put("cookie", account.getCookie());
                result = this.post(params, null, GatewayApiEnums.MAKE_SESSION, MakeSessionResponse.class, account);

                if (result != null) {
                    if (result.getCode().equals(0) && StringUtils.isNotEmpty(result.getSession())) {
                        account.setSession(result.getSession());
                        account.setDeviceinfo(result.getDeviceinfo());
                        account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                        account.setChangeOnlineStatusTime(new Date());
                        account.setLoginError("");
                        account.setIsCheck(true);
                        account.setAccID(result.getProfile().getEmail());
                        account.setEmail(result.getProfile().getEmail());
                        // 判断email是否存在，存在则禁用
                        Account accountByAccID = accountRepository.findByAccID(result.getProfile().getEmail());
                        if (accountByAccID != null && !accountByAccID.get_id().equals(account.get_id()) && accountByAccID.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode())) {
                            account.setSession(result.getSession());
                            account.setDeviceinfo(result.getDeviceinfo());
                            account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
                            account.setChangeOnlineStatusTime(new Date());
                            account.setLoginError("账号离线：账号已存在");
                            account.setIsCheck(true);
                            account.setAccID(UUIDUtils.get32UUId());
                            account.setEmail(result.getProfile().getEmail());
                        }
                        accountRepository.updateLoginSuccess(account);
                        if (account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode())) {
                            this.removeFilters(account);
                        }
                        break;
                    }

                    if (result.getCode().equals(-400)) {
                        break;
                    }
                }

                log.info("makeSession 请求失败 {} {}",account.get_id(), account.getEmail());

                Thread.sleep(1000);
                account.setSocks5Id("");
            } catch (CommonException e) {
                account.setChangeOnlineStatusTime(new Date());
                account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
                account.setLoginError("账号离线：" + e.getMessage());
                accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
                throw e;
            } catch (Exception e) {
                log.info("makeSession ERROR: " + e.getMessage());
            }
        }
        if (result == null || !result.getCode().equals(0)) {
            account.setChangeOnlineStatusTime(new Date());
            account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
            account.setLoginError((result != null && result.getCode().equals(-400) ? "账号被封：" : "账号离线：") + (result == null ? "请求失败1" : result.getMsg()));
            accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
        }

        return result;
    }


    public GetInboxEmailListResponse getInboxEmailList(Integer page, Account account) {
        if (page == null) {
            page = 0;
        }
        GetInboxEmailListResponse result = null;
        for (int i = 0; i < 3; i++) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("page", page);
                result = this.post(params, account.getSession(), GatewayApiEnums.GET_INBOX_EMAIL_LIST, GetInboxEmailListResponse.class, account);
                if (result != null && result.getCode().equals(0)) {
                    if (!account.getIsCheck()) {
                        account.setIsCheck(true);
                        accountRepository.updateLoginSuccess(account);
                    }
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

    public GetEmailDetailResponse getEmailDetail(String threadId, List<String> messageIds, Account account) {
        GetEmailDetailResponse result = null;
        for (int i = 0; i < 3; i++) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("thread_id", threadId);
                if (messageIds != null && messageIds.size() > 0) {
                    params.put("message_ids", messageIds);
                }
                OtherGatewayResultBase reponseStr = this.post(Map.of("threads", List.of(params)), account.getSession(), GatewayApiEnums.GET_EMAIL_DETAIL, null, account);
                if (reponseStr != null) {
                    result = GetEmailDetailResponse.buildFromJson(reponseStr.getAllResponseStr());
                    if (result.getCode().equals(0)) {
                        return result;
                    }
                }
            } catch (CommonException e) {
                throw e;
            } catch (Exception e) {
                log.info("getEmailDetail ERROR: " + e.getMessage());
            }
        }

        return result;
    }

    public SendEmailResponse sendEmail(String subject, String sender_name, List<String> receivers, String content, Account account) {
        SendEmailResponse result = null;
        for (int i = 0; i < 3; i++) {
            try {
                if (account.getType() != null && account.getType().equals(AccountTypeEnums.web.getCode())) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("subject", subject);
                    params.put("sender_name", sender_name);
                    params.put("receivers", receivers);
                    params.put("content", content);
                    result = this.post(params, account.getSession(), GatewayApiEnums.SEND_EMAIL, SendEmailResponse.class, account);
                    if (result != null && result.getCode().equals(0)) {
                        return result;
                    }
                }
            } catch (CommonException e) {
                throw e;
            } catch (Exception e) {
                log.info("sendEmail ERROR: " + e.getMessage());
            }
        }

        return result;
    }

    public void removeFilters(Account account) {
        for (int i = 0; i < 3; i++) {
            try {
                if (account.getType() != null && account.getType().equals(AccountTypeEnums.web.getCode())) {
                    Map<String, Object> params = new HashMap<>();
                    GatewayResultBase result = this.post(params, account.getSession(), GatewayApiEnums.REMOVE_FILTERS, GatewayResultBase.class, account);
                    if (result != null && result.getCode().equals(0)) {
                        return ;
                    }
                }
            } catch (CommonException e) {
                throw e;
            } catch (Exception e) {
                log.info("sendEmail ERROR: " + e.getMessage());
            }
        }
    }

//    public SendEmailResponse sendEmail(String subject, String sender_name, List<String> receivers, String content, Account account) {
//        SendEmailResponse response = _sendEmail(subject, sender_name, receivers, content, account);
//        if (response != null && response.getCode().equals(0)) {
//            for (int i = 0; i < 10; i++) {
//                try {
//                    Account account1 = accountRepository.findById(account.get_id());
//                    account1.setSendEmailTotal(account1.getSendEmailTotal() + 1);
//                    accountRepository.update(account1);
//                    break;
//                } catch (Exception e) {
//                }
//            }
//        }
//        return response;
//    }
}
