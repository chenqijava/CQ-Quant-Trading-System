package com.nyy.gmail.cloud.gateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
@Component
public class OutlookGraphGatewayClient {

    @Resource
    private Socks5Service socks5Service;

    @Resource
    private AccountRepository accountRepository;

    @Autowired
    private ProxyAccountService proxyAccountService;

    @Autowired
    private Socks5Repository socks5Repository;

    public OkHttpClient getHttpClient(Socks5 socks5) {
        return OkHttpClientFactory.getSendGrid(socks5);
    }

    private void updateSession (String id, String accessToken) {
        if (accountRepository != null) {
            accountRepository.updateSession(id, accessToken);
        }
    }

    public String getAccessToken(Socks5 socks5, Account account) {
        String accessToken = "";
        String error = "";
        try {
            OkHttpClient httpClient = getHttpClient(socks5);

            FormBody formBody = new FormBody.Builder()
                    .add("client_id", account.getOutlookGraphClientId())
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", account.getOutlookGraphRefreshToken())
                    .add("scope", "https://graph.microsoft.com/.default")
                    .build();

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://login.microsoftonline.com/consumers/oauth2/v2.0/token");
            request = builder.post(formBody).build();

            log.info("_id {} api: {}", account.get_id(), "getAccessToken");

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String respStr = response.body().string();
                    log.info("_id {} api: {} response:{}", account.get_id(), "getAccessToken", respStr.substring(0, Math.min(respStr.length(), 1000)));
                    error = "code:" + response.code() + " response:" + respStr;
                    JSONObject jsonObject = JSON.parseObject(respStr);
                    accessToken = jsonObject.getString("access_token");
                    account.setSession(accessToken);
                    updateSession(account.get_id(), accessToken);
                    return accessToken;
                } else {
                    log.error("_id {} api: getAccessToken Request failed http status code: {}", account.get_id(), response.code());
                }
            } catch (IOException e) {
                log.error("_id {} api: getAccessToken Request failed error: {}", account.get_id(), e.getMessage());
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (StringUtils.isNotBlank(accessToken)) {
                account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                account.setChangeOnlineStatusTime(new Date());
                account.setLoginError("");
                account.setIsCheck(true);
                if (accountRepository != null) {
                    accountRepository.updateLoginSuccess(account);
                }
            } else {
                if (StringUtils.isNotBlank(error)) {
                    account.setOnlineStatus(AccountOnlineStatus.OFFLINE.getCode());
                    account.setChangeOnlineStatusTime(new Date());
                    account.setLoginError(error);
                    account.setIsCheck(true);
                    if (accountRepository != null) {
                        accountRepository.updateOnlineStatus(account.get_id(), account.getOnlineStatus(), account.getLoginError());
                    }
                }
            }
        }
    }

    public List<OutlookGraphFolder> getFolders (Account account) {
        for (int i = 0; i < 3; i++) {
            try {
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

                String accessToken = account.getSession();
                if (StringUtils.isEmpty(accessToken)) {
                    accessToken = getAccessToken(socks5, account);
                }
                if (StringUtils.isNotEmpty(accessToken)) {
                    OkHttpClient httpClient = getHttpClient(socks5);

                    Request request = null;
                    Request.Builder builder = new Request.Builder().url("https://graph.microsoft.com/v1.0/me/mailFolders?$select=id,displayName");
                    request = builder.get().addHeader("Authorization", "Bearer " + accessToken).build();

                    log.info("_id {} api: {} params: {}", account.get_id(), "getFolders", "");
                    ArrayList<OutlookGraphFolder> outlookGraphFolders = null;

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.code() == 401) {
                            account.setSession("");
                            updateSession(account.get_id(), "");
                        } else {
                            if (response.code() == 200 && response.body() != null) {
                                String respStr = response.body().string();
                                log.info("_id {} api: {} response:{}", account.get_id(), "getFolders", respStr.substring(0, Math.min(respStr.length(), 1000)));

                                JSONObject jsonObject = JSON.parseObject(respStr);
                                JSONArray jsonArray = jsonObject.getJSONArray("value");
                                outlookGraphFolders = new ArrayList<>();
                                for (int j = 0; j < jsonArray.size(); j++) {
                                    OutlookGraphFolder email = jsonArray.getObject(j, OutlookGraphFolder.class);
                                    outlookGraphFolders.add(email);
                                }
                                return outlookGraphFolders;
                            } else {
                                log.error("_id {} api: getFolders Request failed http status code: {}", account.get_id(), response.code());
                            }
                        }
                    } catch (IOException e) {
                        log.error("_id {} api: getFolders Request failed error: {}", account.get_id(), e.getMessage());
                    } finally {
                        if (outlookGraphFolders == null && StringUtils.isNotEmpty(account.getSession())) {
                            account.setSession("");
                            updateSession(account.get_id(), "");
                        }
                    }
                }
            } catch (CommonException e) {
                return new ArrayList<>();
            } catch (Exception e) {
                log.error("_id {} api: getFolders failed error: {}", account.get_id(), e.getMessage());
            }
            account.setSocks5Id("");
        }

        return new ArrayList<>();
    }

    public List<OutlookGraphEmail> getEmails (Account account, int top) {
        for (int i = 0; i < 3; i++) {
            try {
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

                String accessToken = account.getSession();
                if (StringUtils.isEmpty(accessToken)) {
                    accessToken = getAccessToken(socks5, account);
                }
                if (StringUtils.isNotEmpty(accessToken)) {
                    OkHttpClient httpClient = getHttpClient(socks5);

                    Request request = null;
                    Request.Builder builder = new Request.Builder().url(top <= 0 ? "https://graph.microsoft.com/v1.0/me/messages" : "https://graph.microsoft.com/v1.0/me/messages?$top="+top+"&$orderby=receivedDateTime desc");
                    request = builder.get().addHeader("Authorization", "Bearer " + accessToken).build();

                    log.info("_id {} api: {} params: {}", account.get_id(), "getEmails", "");
                    ArrayList<OutlookGraphEmail> outlookGraphEmails = null;

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.code() == 401) {
                            account.setSession("");
                            updateSession(account.get_id(), "");
                        } else {
                            if (response.code() == 200 && response.body() != null) {
                                String respStr = response.body().string();
                                log.info("_id {} api: {} response:{}", account.get_id(), "getEmails", respStr.substring(0, Math.min(respStr.length(), 1000)));

                                JSONObject jsonObject = JSON.parseObject(respStr);
                                JSONArray jsonArray = jsonObject.getJSONArray("value");
                                outlookGraphEmails = new ArrayList<>();
                                for (int j = 0; j < jsonArray.size(); j++) {
                                    OutlookGraphEmail email = jsonArray.getObject(j, OutlookGraphEmail.class);
                                    outlookGraphEmails.add(email);
                                }

                                if (!account.getIsCheck()) {
                                    account.setIsCheck(true);
                                    if (accountRepository != null) {
                                        accountRepository.updateLoginSuccess(account);
                                    }
                                }
                                return outlookGraphEmails;
                            } else {
                                log.error("_id {} api: getEmails Request failed http status code: {}", account.get_id(), response.code());
                            }
                        }
                    } catch (IOException e) {
                        log.error("_id {} api: getEmails Request failed error: {}", account.get_id(), e.getMessage());
                    } finally {
                        if (outlookGraphEmails == null && StringUtils.isNotEmpty(account.getSession())) {
                            account.setSession("");
                            updateSession(account.get_id(), "");
                        }
                    }
                }
            } catch (CommonException e) {
                return new ArrayList<>();
            } catch (Exception e) {
                log.error("_id {} api: getEmails failed error: {}", account.get_id(), e.getMessage());
            }
            account.setSocks5Id("");
        }

        return new ArrayList<>();
    }

    public List<OutlookGraphEmail> getJunkEmails (Account account, int top) {
        for (int i = 0; i < 3; i++) {
            try {
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

                String accessToken = account.getSession();
                if (StringUtils.isEmpty(accessToken)) {
                    accessToken = getAccessToken(socks5, account);
                }
                if (StringUtils.isNotEmpty(accessToken)) {
                    OkHttpClient httpClient = getHttpClient(socks5);

                    Request request = null;
                    Request.Builder builder = new Request.Builder().url(top <= 0 ? "https://graph.microsoft.com/v1.0/me/mailFolders/junkemail/messages" : "https://graph.microsoft.com/v1.0/me/mailFolders/junkemail/messages?$top="+top+"&$orderby=receivedDateTime desc");
                    request = builder.get().addHeader("Authorization", "Bearer " + accessToken).build();

                    log.info("_id {} api: {} params: {}", account.get_id(), "getEmails", "");
                    ArrayList<OutlookGraphEmail> outlookGraphEmails = null;

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.code() == 401) {
                            account.setSession("");
                            updateSession(account.get_id(), "");
                        } else {
                            if (response.code() == 200 && response.body() != null) {
                                String respStr = response.body().string();
                                log.info("_id {} api: {} response:{}", account.get_id(), "getEmails", respStr.substring(0, Math.min(respStr.length(), 1000)));

                                JSONObject jsonObject = JSON.parseObject(respStr);
                                JSONArray jsonArray = jsonObject.getJSONArray("value");
                                outlookGraphEmails = new ArrayList<>();
                                for (int j = 0; j < jsonArray.size(); j++) {
                                    OutlookGraphEmail email = jsonArray.getObject(j, OutlookGraphEmail.class);
                                    outlookGraphEmails.add(email);
                                }
                                return outlookGraphEmails;
                            } else {
                                log.error("_id {} api: getEmails Request failed http status code: {}", account.get_id(), response.code());
                            }
                        }
                    } catch (IOException e) {
                        log.error("_id {} api: getEmails Request failed error: {}", account.get_id(), e.getMessage());
                    } finally {
                        if (outlookGraphEmails == null && StringUtils.isNotEmpty(account.getSession())) {
                            account.setSession("");
                            updateSession(account.get_id(), "");
                        }
                    }
                }
            } catch (CommonException e) {
                return new ArrayList<>();
            } catch (Exception e) {
                log.error("_id {} api: getEmails failed error: {}", account.get_id(), e.getMessage());
            }
            account.setSocks5Id("");
        }

        return new ArrayList<>();
    }

    public boolean sendEmail (String subject, List<String> receivers, String content, Account account) {
        for (int i = 0; i < 3; i++) {
            try {
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

                String accessToken = account.getSession();
                if (StringUtils.isEmpty(accessToken)) {
                    accessToken = getAccessToken(socks5, account);
                }
                if (StringUtils.isNotEmpty(accessToken)) {
                    OkHttpClient httpClient = getHttpClient(socks5);

                    MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                    String jsonBody = JSON.toJSONString(Map.of("message", Map.of("subject", subject, "body",
                                    Map.of("contentType", "HTML", "content", content),
                                    "toRecipients", receivers.stream().map(e -> Map.of("emailAddress", Map.of("address", e))).toList()),
                            "saveToSentItems", "true"));

                    RequestBody body = RequestBody.create(jsonBody, mediaType);

                    Request request = null;
                    Request.Builder builder = new Request.Builder().url("https://graph.microsoft.com/v1.0/me/sendMail");
                    request = builder.post(body).addHeader("Authorization", "Bearer " + accessToken).build();

                    log.info("_id {} api: {} params: {}", account.get_id(), "sendEmail", jsonBody.substring(0, Math.min(jsonBody.length(), 1000)));
                    Boolean result = null;

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.code() >= 200 && response.code() < 300) {
                            result = true;
                            return true;
                        } else {
                            if (response.code() == 429 || response.code() == 403) {
                                if (accountRepository != null) {
                                    accountRepository.updateLimitSendEmail(account.get_id());
                                }
                                throw new CommonException(ResultCode.SEND_MAIL_FAIL, response.body().string());
                            }
                            throw new RuntimeException("code: " + response.code() + " response: " + response.body().string());
                        }
                    } catch (IOException e) {
                        log.error("_id {} api: sendEmail Request failed error: {}", account.get_id(), e.getMessage());
                    } finally {
                        if (result == null) {
                            account.setSession("");
                            updateSession(account.get_id(), "");
                        }
                    }
                }
            } catch (CommonException e) {
                log.error("_id {} api: sendEmail failed error: {}", account.get_id(), e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("_id {} api: sendEmail failed error: {}", account.get_id(), e.getMessage());
            }
            account.setSocks5Id("");
        }

        return false;
    }

    public static void main(String[] args) {
        Account account = new Account();
        account.setProxyIp("155.94.135.245");
        account.setProxyPort("10240");
        account.setProxyUsername("eric");
        account.setProxyPassword("ericss10238");
        account.setOutlookGraphClientId("9e5f94bc-e8a4-4e73-b8be-63364c29d753");
        account.setOutlookGraphRefreshToken("M.C516_BAY.0.U.-CrJGtzsYLqSKKtrIrQJ5S84Rzs1BdPP3rYFDCDL0cbEBNz660AlSQQJVgB!QduyxOWZqURVmXj9qNMJghyXaLRVQk6ijVPjhbZPOHjozgid1kwLF6hel!BpvVf5Yl7kuJ72Qrn5b6cjBgv!u3WeCGjPdYchGeHrqOsiuH6waoWhI27d4vhYH0XoZ3DberepdK0N!8qWIsJMiMIGK9OiVzJ6Vj7aBhV7UAETCqCpBxx7Fnu2PA2up!W9Gm2vhklOCrsHA7WqH2DQkOm0sgNRXk0Ae7bKeAlRpHPijmO3Hc9rnVNX21O7IDKBPJDwa!bGzCP!3mAetGzt2kI2d!!YO05sU4he2mnviTEFGhVegF4yCnO82K3ZXXkdf*PHHa1nhfNtgk*srLGWD80Zp2b3e6D1ilGxuTffmtwt!NaXexPfU");

        OutlookGraphGatewayClient outlookGraphGatewayClient = new OutlookGraphGatewayClient();
//        outlookGraphGatewayClient.sendEmail("HELLO", List.of("c1311949161@gmail.com"), "Good Morning", account);
//        List<OutlookGraphEmail> emails = outlookGraphGatewayClient.getJunkEmails(account, 0);
//        List<OutlookGraphEmail> emails = outlookGraphGatewayClient.getEmails(account, 0);
//        System.out.println(emails);
        List<OutlookGraphFolder> folders = outlookGraphGatewayClient.getFolders(account);
        System.out.println(folders);
    }
}
