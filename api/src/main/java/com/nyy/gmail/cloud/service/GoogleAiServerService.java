package com.nyy.gmail.cloud.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.GoogleAiServer;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.repository.mongo.GoogleAiServerRepository;
import com.nyy.gmail.cloud.utils.SignGenerator;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoogleAiServerService {

    @Autowired
    private GoogleAiServerRepository googleAiServerRepository;


    public void save(GoogleAiServer reqDTO, String userID) {
        if (StringUtils.isEmpty(reqDTO.getUrl()) || StringUtils.isEmpty(reqDTO.getApiKey()) || StringUtils.isEmpty(reqDTO.getApiSecret())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (StringUtils.isEmpty(reqDTO.get_id())) {
            GoogleAiServer googleAiServer = new GoogleAiServer();
            googleAiServer.setApiKey(reqDTO.getApiKey());
            googleAiServer.setApiSecret(reqDTO.getApiSecret());
            googleAiServer.setUrl(reqDTO.getUrl());
            googleAiServer.setUserID(userID);
            googleAiServer.setName(reqDTO.getName());
            googleAiServer.setCreateTime(new Date());
            googleAiServerRepository.save(googleAiServer);
        } else {
            GoogleAiServer googleAiServer = googleAiServerRepository.findById(reqDTO.get_id());
            if (googleAiServer != null && googleAiServer.getUserID().equals(userID)) {
                googleAiServer.setApiKey(reqDTO.getApiKey());
                googleAiServer.setApiSecret(reqDTO.getApiSecret());
                googleAiServer.setUrl(reqDTO.getUrl());
                googleAiServer.setName(reqDTO.getName());
                googleAiServerRepository.update(googleAiServer);
            }
        }
    }

    public void delete(IdsListDTO ids, String userID) {
        for (String id : ids.getIds()) {
            GoogleAiServer googleAiServer = googleAiServerRepository.findById(id);
            if (googleAiServer != null && userID.equals(googleAiServer.getUserID())) {
                googleAiServerRepository.delete(googleAiServer);
            }
        }
    }

    public GoogleAiServer selectOneServer(boolean force) {
        List<GoogleAiServer> all = googleAiServerRepository.findAll();
        List<GoogleAiServer> list = all.stream().filter(e -> e.getScore() != null && e.getScore() >= 0.65).toList();
        if (list.isEmpty()) {
            if (force) {
                list = all;
            } else {
                return null;
            }
        }
        Random random = new Random();
        int index = random.nextInt(list.size());
        return list.get(index);
    }

    public GoogleAiServer findById(String serverId) {
        return googleAiServerRepository.findById(serverId);
    }

    public void uploadSever() {
        List<GoogleAiServer> all = googleAiServerRepository.findAll();
        for (GoogleAiServer googleAiServer : all) {
            try {
                OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
                // JSON 请求体
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                Map<String, Object> reqDto = new HashMap<>();
                String jsonBody = JSON.toJSONString(reqDto);

                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, mediaType);

                Request request = null;
                Request.Builder builder = new Request.Builder().url(googleAiServer.getUrl() + "/api/open/serverScore");
                builder.addHeader("X-API-KEY", googleAiServer.getApiKey());
                builder.addHeader("X-SIGN", SignGenerator.generateSign(reqDto, googleAiServer.getApiSecret()));
                request = builder.post(body).build();

                try (Response response = defaultClient.newCall(request).execute()) {
                    if (response.body() != null) {
                        String respStr = response.body().string();

                        /**
                         * map.put("score", score);
                         *         map.put("throughput", l2 * 1.0);
                         *         map.put("noImages", l * 10.0);
                         */
                        JSONObject jsonObject = JSONObject.parseObject(respStr);
                        jsonObject = jsonObject.getJSONObject("data");
                        String score = jsonObject.getString("score");
                        String throughput = jsonObject.getString("throughput");
                        String noImages = jsonObject.getString("noImages");
                        String enableKeyNum = jsonObject.getString("enableKeyNum");
                        String enableKeyGoogleNum = jsonObject.getString("enableKeyGoogleNum");
                        String enableKeyChatgptNum = jsonObject.getString("enableKeyChatgptNum");

                        googleAiServer.setScore(Double.parseDouble(score));
                        googleAiServer.setThroughput(Double.parseDouble(throughput));
                        googleAiServer.setNoImages(Double.parseDouble(noImages));
                        if (StringUtils.isNotBlank(enableKeyNum)) {
                            googleAiServer.setEnableKeyNum(Double.parseDouble(enableKeyNum));
                        }
                        if (StringUtils.isNotBlank(enableKeyGoogleNum)) {
                            googleAiServer.setEnableKeyGoogleNum(Double.parseDouble(enableKeyGoogleNum));
                        }
                        if (StringUtils.isNotBlank(enableKeyChatgptNum)) {
                            googleAiServer.setEnableKeyChatgptNum(Double.parseDouble(enableKeyChatgptNum));
                        }

                        googleAiServerRepository.update(googleAiServer);
                    }
                } catch (IOException e) {
                    log.error("Request failed error: {}", e.getMessage());
                }
            }  catch (Exception e) {
                log.error("Request failed error: {}", e.getMessage());
            }
        }
    }
}
