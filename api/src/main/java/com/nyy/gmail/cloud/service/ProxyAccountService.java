package com.nyy.gmail.cloud.service;

import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.enums.Socks5TypeEnums;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.entity.mongo.ProxyAccount;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.repository.mongo.ProxyAccountRepository;
import com.nyy.gmail.cloud.repository.mongo.Socks5Repository;
import com.nyy.gmail.cloud.utils.HttpUtil;
import com.nyy.gmail.cloud.utils.PhoneUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProxyAccountService {
    @Value("${config.ipPlatform}")
    String aggregationServerUrl;

    public static List<String> DEFAULT_SOCKS_AREA = List.of("US", "JP", "GB", "DE", "FR", "KR", "AU");

    private Map<String, List<ProxyAccount>> userId2PA = new HashMap<>();
    private Map<String, AtomicInteger> counter = new ConcurrentHashMap<>();

    @Autowired
    private ProxyAccountRepository proxyAccountRepository;
    @Autowired
    private Socks5Repository socks5Repository;
    @Autowired
    private SocksChangeService socksChangeService;
    @Autowired
    private ParamsService paramsService;


    @PostConstruct
    public void init() {
        this.reloadActives();
    }

    public void reloadActives() {
        List<ProxyAccount> pas = proxyAccountRepository.findProxyAccountByStatus(true);
        userId2PA = pas.stream()
                .collect(Collectors.groupingBy(ProxyAccount::getUserID,
                        Collectors.toCollection(CopyOnWriteArrayList::new)));
        log.info("proxy account loaded {}:{}", pas.size(), userId2PA);
    }

    public String getUserID() {
        Session session = Session.currentSession();
        if (session != null && StringUtils.isNotEmpty(session.getUserID())) {
            return session.getUserID();
        }
        return "admin";
    }

    public String getIpPlatforms() {
        String url = String.format("%s/api/socks5/getPlatform", aggregationServerUrl);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try (Response response = OkHttpClientFactory.getDefaultClient().newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        } catch (IOException e) {
            log.error("", e);
        }
        return null;
    }

    private Socks5 getProxyFromPA(String area, ProxyAccount pa) {
        String url = String.format("%s/api/socks5/getProxy", aggregationServerUrl);
        JSONObject jsonBody = new JSONObject();
        String batchId = area;
        jsonBody.put("traceId", UUID.randomUUID().toString());
        jsonBody.put("batchid", batchId);
        jsonBody.put("platform", pa.getToken());
        log.debug("request {}:{}", url, jsonBody);

        for (int n = 0; n < 3; n++) {
            try {
                JSONObject result = HttpUtil.postJson(url, jsonBody);
                log.debug("response {}:{}", url, result);
                if (result == null) {
                    continue;
                }
                if (result.getIntValue("code") != 0) {
                    log.info("get proxy error {}", result);
                    return null;
                }
                JSONObject data = result.getJSONObject("data");
                Socks5 socks5 = new Socks5();
                socks5.setIp(data.getString("ip"));
                socks5.setPort(data.getIntValue("port"));
                socks5.setUsername(data.getString("username"));
                socks5.setPassword(data.getString("password"));
                socks5.setArea(area);
                socks5.setBatchid(batchId);
                socks5.setUseCount(pa.getMaxVpsNum());
                socks5.setProxyAccount(pa.getAccount());
                socks5Repository.saveSocks5(socks5);
                return socks5;
            } catch (IOException e) {
            }
        }
        return null;
    }

    public Socks5 getDynamicProxy(List<String> areaList, String userID) {
        List<ProxyAccount> paList = userId2PA.get(userID);
        if (CollectionUtils.isEmpty(areaList) || CollectionUtils.isEmpty(paList)) {
            return null;
        }

        for (String area : areaList) {
            int startIndex = counter.getOrDefault(userID, new AtomicInteger(0)).incrementAndGet();
            for (int i = 0; i < paList.size(); i++) {
                int index = (startIndex + i) % paList.size();
                Socks5 socks5 = getProxyFromPA(area, paList.get(index));
                if (socks5 != null) {
                    return socks5;
                }
            }
        }
        return null;
    }

    public Socks5 getStaticProxy(List<String> areaList, String account_id, String userID) {
        int max = paramsService.getSocks5UseMax();
        Socks5 socks5 = socks5Repository.findAndUse(areaList, account_id, max, userID);
        return socks5;
    }

    private String randomDefaultArea() {
        return DEFAULT_SOCKS_AREA.get(RandomUtils.nextInt(0, DEFAULT_SOCKS_AREA.size()));
    }

    public Socks5 getProxy(String account_id, String userID) {
        return getProxy(account_id, userID,null);
    }

    public Socks5 getProxy(String account_id, String userID, String phoneNumber) {
        String area = StringUtils.isEmpty(phoneNumber) ? null : PhoneUtil.getRegionCodeForPhone(phoneNumber);
        area = StringUtils.isEmpty(area) ? randomDefaultArea() : area;
        List<String> areaList = Collections.singletonList(area);
        log.info("account {} phone {} generate area:{}", account_id, phoneNumber, areaList);

        int type = Socks5TypeEnums.All.getValue();

        List<String> allChangeAreas = areaList.stream()
                .map(a -> {
                    List<String> changeAreas = socksChangeService.getSocksChange(a, userID);
                    return CollectionUtils.isEmpty(changeAreas) ? Collections.singletonList(a) : changeAreas;
                }).flatMap(List::stream).toList();

        if ((type & Socks5TypeEnums.DynamicProxy.getValue()) > 0) {
            Socks5 socks5 = getDynamicProxy(allChangeAreas, userID);
            if (socks5 != null) {
                return socks5;
            }
        }

        if ((type & Socks5TypeEnums.StaticProxy.getValue()) > 0) {
            releaseProxy(account_id);
            Socks5 socks5 = getStaticProxy(Collections.emptyList(), account_id, userID);
            if (socks5 != null) {
                return socks5;
            }
        }
        log.info("account {} phone {} areas:{} no valid proxy", account_id, phoneNumber, areaList);
        return null;
    }

    public void releaseProxy(String account_id) {
        if (StringUtils.isEmpty(account_id)) {
            return;
        }
        socks5Repository.findAndRelease(account_id);
    }
}

