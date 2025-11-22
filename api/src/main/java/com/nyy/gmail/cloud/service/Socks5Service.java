package com.nyy.gmail.cloud.service;

import com.alibaba.fastjson2.JSON;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.enums.Socks5StatusEnum;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.AccountOtherStatusTypeEnums;
import com.nyy.gmail.cloud.enums.IpCheckStatusEnum;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.Socks5Repository;

import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.utils.CountryCodeEnum;
import com.nyy.gmail.cloud.utils.GeoIPCountryUtils;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class Socks5Service {
    public static final String MY_IP_URL = "https://cgi1.apnic.net/cgi-bin/my-ip.php";
    public static final String IFCONFIG_URL = "https://ifconfig.io/ip";

    @Autowired
    private Socks5Repository socks5Repository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private final static int socksUseLimit = 50;

    @Autowired
    private ProxyAccountService proxyAccountService;

    @Async("http")
    public void checkNetwork(Socks5 socks5) {
        OkHttpClient client = OkHttpClientFactory.getSocks5Client(socks5);
        Request request = new Request.Builder().url(MY_IP_URL).get().build();
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                socks5.setStatusFlag(Socks5StatusEnum.NETERROR.getValue());
//                socks5.setStatus(e.getMessage());
//                socks5.setLastCheckTime(System.currentTimeMillis());
//                log.info("{} {} fail {}", socks5.getIp(), socks5.get_id(), e.getMessage());
//                socks5Repository.updateSocks5(socks5);
//            }
//
//            @Override
//            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
//                socks5.setStatusFlag(Socks5StatusEnum.OK.getValue());
//                socks5.setStatus(Socks5StatusEnum.OK.name());
//                socks5.setLastCheckNormalTime(System.currentTimeMillis());
//                socks5.setLastCheckTime(System.currentTimeMillis());
//                log.info("{} {} success {}", socks5.getIp(),socks5.get_id(), response.body().string());
//                socks5Repository.updateSocks5(socks5);
//            }
//        });
        long begin = System.currentTimeMillis();
        String desc = "";
        try (Response response = client.newCall(request).execute()) {
            socks5.setStatusFlag(Socks5StatusEnum.OK.getValue());
            socks5.setStatus(Socks5StatusEnum.OK.name());
            socks5.setLastCheckNormalTime(System.currentTimeMillis());
            socks5.setIpCheckStatus(IpCheckStatusEnum.CHECKED_SUCCESS.getCode());
            desc = response.body().string().replace("\n", "");
            String area = GeoIPCountryUtils.getCountryCode(socks5.getIp());
            socks5.setArea(area);
            if (StringUtils.isNotBlank(area)) {
                socks5.setCountryName(CountryCodeEnum.fromCode(area).getChineseName());
            }
//            redisTemplate.opsForSet().add(Constants.VALID_IP_LIST_KEY, genSocks5Key(socks5));
        } catch (IOException e) {
            socks5.setStatusFlag(Socks5StatusEnum.NETERROR.getValue());
            socks5.setStatus(e.getMessage());
            socks5.setIpCheckStatus(IpCheckStatusEnum.CHECKED_FAIL.getCode());
//            redisTemplate.opsForSet().remove(Constants.VALID_IP_LIST_KEY, socks5);
        } finally {
            socks5.setLastCheckTime(System.currentTimeMillis());
            socks5Repository.updateSocks5(socks5);
        }
    }


    private String genSocks5Key(Socks5 socks5) {
        return socks5.getIp() + ":" + socks5.getPort() + ":" + socks5.getUsername() + ":" + socks5.getPassword();
    }


    public Socks5 getAccountSocks(Account account) {
        if (!StringUtils.isEmpty(account.getSocks5Id())) {
            Socks5 socks5 = socks5Repository.findSocks5ById(account.getSocks5Id());
            if (socks5 != null && socks5.getStatusFlag() != Socks5StatusEnum.NETERROR.getValue()) {
                return socks5;
            }
        }

        User user = userRepository.findOneByUserID(account.getUserID());
        if (user == null) {
            throw new CommonException(ResultCode.ERROR);
        }


        String userID = "admin";
        if (user.getSocks5Use().equals("self")) {
            userID = account.getUserID();
        }

        Socks5 socks5 = proxyAccountService.getProxy(account.get_id(), userID);

//        Socks5 socks5 = socks5Repository.findAndUse(new ArrayList<String>(), account.getVpsID(), socksUseLimit, userID);
        if (socks5 == null) {
            throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
        }
        account.setSocks5Id(socks5.get_id());
        accountRepository.updateSocks5Id(account.get_id(), account.getSocks5Id());
        return socks5;
    }

    public void releaseSocks5(String id, String socks5Id, String vpsID) {
        accountRepository.updateSocks5Id(id, "");
        socks5Repository.findAndRelease(id);
    }

    public Socks5CacheInfo parseSocks5Key(String socks5Key) {
        String[] parts = splitSocks5Key(socks5Key);
        return new Socks5CacheInfo(
                parts[0],
                Integer.parseInt(parts[1]),
                parts[2],
                parts[3]
        );
    }

    // 拆解sock5Key
    public static String[] splitSocks5Key(String socks5Key) {
        if (socks5Key == null) {
            return null;
        }

        String[] parts = socks5Key.split(":", 4); // 限制分割4部分
        if (parts.length != 4) {
            return null;
        }

        // 验证port是数字
        try {
            Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        return parts;
    }

    @Async("dealInvalidSocks5")
    public void dealInvalidSocks5Job(Socks5 socks5) {
        if (null == socks5) {
            return;
        }

        // socks5失效的场景
        if (Socks5StatusEnum.NETERROR.getValue() == socks5.getStatusFlag()) {
            // 根绝sock5查询相应的account（在线的）
            List<Account> onlineAccountList = accountRepository.findBySocks5IdAndStatus(socks5.get_id(), AccountOnlineStatus.ONLINE.getCode());
            if (CollectionUtils.isEmpty(onlineAccountList)) {
                return;
            }
            // 如果能查询出来，说明需要统一下线
            for (Account account : onlineAccountList) {
            }
        } else if (Socks5StatusEnum.OK.getValue() == socks5.getStatusFlag()) {
        } else {
            log.warn("socks5 statusFlag is not valid, socks5:{}", JSON.toJSONString(socks5));
        }
    }

    public void releaseSocks5(Account account) {
        account.setSocks5Id("");
        accountRepository.updateSocks5IdAndProxyIp(account.get_id(), account.getSocks5Id(), account.getProxyIp());
        socks5Repository.findAndRelease(account.get_id());
    }

    public Socks5 findCanUse() {
        return socks5Repository.findAndUse();
    }

    @Data
    public static class Socks5CacheInfo implements Serializable {
        private final String ip;
        private final int port;
        private final String username;
        private final String password;

    }


}

