package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.model.dto.ApiKeyReqDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nyy.gmail.cloud.entity.mysql.ApiKey;
import com.nyy.gmail.cloud.repository.mysql.ApiKeyRepository;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
public class ApiKeyService {
    @Autowired
    private ApiKeyRepository apiKeyRepository;

    public ApiKey getApiKey(String apiKey) {
        return apiKeyRepository.findByApiKey(apiKey);
    }

    public void update(ApiKeyReqDTO reqDTO) {
        ApiKey apiKey = apiKeyRepository.findById(reqDTO.getId()).orElse(null);
        if (apiKey == null || !apiKey.getUserID().equals(reqDTO.getUserID())) {
            throw new CommonException(ResultCode.DATA_NOT_EXISTED);
        }
        apiKey.setReceiveCallbackUrl(reqDTO.getReceiveCallbackUrl() == null ? "" : reqDTO.getReceiveCallbackUrl());
        apiKey.setWhiteIp(reqDTO.getWhiteIp());
        apiKeyRepository.save(apiKey);
    }

    public void delete(ApiKeyReqDTO reqDTO) {
        if (reqDTO.getIds() != null && !reqDTO.getIds().isEmpty()) {
            List<ApiKey> all = apiKeyRepository.findAllById(reqDTO.getIds());
            for (ApiKey apiKey : all) {
                if (apiKey.getUserID().equals(reqDTO.getUserID())) {
                    apiKeyRepository.delete(apiKey);
                }
            }
        }
    }

    private static String generateRandomBase64(int length) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static final int API_KEY_LENGTH = 24;     // 生成24字节的API_KEY
    private static final int SECRET_KEY_LENGTH = 48;  // 生成48字节的API_SECRET

    public ApiKey add(ApiKeyReqDTO reqDTO) {
        String userID = Session.currentSession().getUserID();
        ApiKey apiKey = new ApiKey();

        apiKey.setApiSecret(generateRandomBase64(SECRET_KEY_LENGTH));
        apiKey.setApiKey(generateRandomBase64(API_KEY_LENGTH));
        apiKey.setUserID(userID);
        apiKey.setWhiteIp(reqDTO.getWhiteIp());
        apiKey.setReceiveCallbackUrl(reqDTO.getReceiveCallbackUrl() == null ? "" : reqDTO.getReceiveCallbackUrl());
        apiKey = apiKeyRepository.save(apiKey);
        return apiKey;
    }

    public List<ApiKey> findByUserId(String userID) {
        List<ApiKey> apiKeyList = apiKeyRepository.findByUserIDEquals(userID);
        return apiKeyList;
    }
}