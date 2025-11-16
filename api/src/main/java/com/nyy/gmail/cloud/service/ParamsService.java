package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.entity.mongo.Params;
import com.nyy.gmail.cloud.repository.mongo.ParamsRepository;
import com.nyy.gmail.cloud.utils.PropertiesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ParamsService {
    public static final String KEY_SOCKS5_USE_MAX = "socks5.check.maxVpsNum";
    private static final String DEFAULT_NULL_USER = "";

    @Resource
    private ParamsRepository paramsRepository;

    private Map<String, Map<String, Object>> userId2Params = new HashMap<>();

    @PostConstruct
    public void init() {
        this.reloadActivePA();
    }

    public void reloadActivePA() {
        List<Params> paramsList = paramsRepository.findAll();
        userId2Params = paramsList.stream()
                .collect(Collectors.groupingBy(
                        p -> Objects.requireNonNullElse(p.getUserID(), DEFAULT_NULL_USER),
                        Collectors.toMap(Params::getCode, Params::getValue)
                ));
        log.info("load params:{}", userId2Params);
    }

    public Object getParamsInMem(String code, Object defaultValue, String userID) {
        userID = userID == null ? DEFAULT_NULL_USER : userID;
        return Optional.ofNullable(userId2Params.get(userID))
                .map(innerMap -> innerMap.get(code))
                .orElse(defaultValue);
    }

    public int getSocks5UseMax() {
        return Integer.parseInt((String) getParamsInMem(KEY_SOCKS5_USE_MAX, "10000", null));
    }

    public boolean openSieveActiveTask() {
        return (Boolean) getParamsInMem("task.openSieveActiveTask", false, null);
    }

    public String getCloudMasterURL() {
        return (String) getParamsInMem("server.cloudMasterURL", null, null);
    }

    public String getCloudMasterCookie() {
        return (String) getParamsInMem("server.cloudMasterCookie", null, null);
    }

    public Object getParams(String code, Object defaultValue, String userID) {
        Params query = new Params();
        query.setCode(code);
        if (StringUtils.isNotEmpty(userID)) {
            query.setUserID(userID);
        }

        Optional<Params> params = paramsRepository.findOne(Example.of(query));
        if (params.isPresent()) {
            return params.get().getValue();
        }
        return defaultValue != null ? defaultValue : PropertiesUtil.readProperty("config.defaultParams." + code);
    }

    public void editParams(String code, Object value, String userID) {
        paramsRepository.findOneAndUpdateByValue(code, value, userID);
        this.reloadActivePA();
    }
}
