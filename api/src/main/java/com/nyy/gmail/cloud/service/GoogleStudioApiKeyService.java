package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.GoogleStudioApiKey;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.enums.AiTypeEnums;
import com.nyy.gmail.cloud.model.dto.AccountPlatformReqDto;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.repository.mongo.GoogleStudioApiKeyRepository;
import com.nyy.gmail.cloud.repository.mongo.Socks5Repository;
import com.nyy.gmail.cloud.service.ai.AiImageRecognition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GoogleStudioApiKeyService {

    @Autowired
    private GoogleStudioApiKeyRepository googleStudioApiKeyRepository;

    @Autowired
    private Socks5Service socks5Service;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ProxyAccountService proxyAccountService;

    @Autowired
    private Socks5Repository socks5Repository;

    public void save(GoogleStudioApiKey googleStudioApiKey, String userID) {
        if (StringUtils.isEmpty(googleStudioApiKey.getApiKey()) || StringUtils.isEmpty(googleStudioApiKey.getEmail())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (StringUtils.isEmpty(googleStudioApiKey.getType())) {
            googleStudioApiKey.setType(AiTypeEnums.GoogleStudio.getCode());
        }
        if (StringUtils.isNotEmpty(googleStudioApiKey.get_id())) {
            GoogleStudioApiKey update = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), userID);
            update.setApiKey(googleStudioApiKey.getApiKey());
            update.setEmail(googleStudioApiKey.getEmail());
            update.setType(googleStudioApiKey.getType());
            googleStudioApiKeyRepository.update(update);
        } else {
            GoogleStudioApiKey newAcc = new GoogleStudioApiKey();
            newAcc.setUserID(userID);
            newAcc.setCreateTime(new Date());
            newAcc.setApiKey(googleStudioApiKey.getApiKey());
            newAcc.setEmail(googleStudioApiKey.getEmail());
            newAcc.setType(googleStudioApiKey.getType());
            googleStudioApiKeyRepository.save(newAcc);
        }
    }

    public void delete(List<String> ids, String userID) {
        List<GoogleStudioApiKey> platforms = googleStudioApiKeyRepository.findByIdIn(ids, userID);
        for (GoogleStudioApiKey platform : platforms) {
            socks5Service.releaseSocks5(platform.get_id(), platform.getSocks5Id(), null);
            googleStudioApiKeyRepository.delete(platform);
        }
    }

    public PageResult<GoogleStudioApiKey> list(AccountPlatformReqDto data, String userID, int pageSize, int page) {
        PageResult<GoogleStudioApiKey> pagination = googleStudioApiKeyRepository.findByPagination(data, pageSize, page);
        return pagination;
    }

    public List<GoogleStudioApiKey> findAll(String userID) {
        return googleStudioApiKeyRepository.findByUserID(userID);
    }

    public int saveBatch(List<GoogleStudioApiKey> entities) {
        // 先查询
        List<String> apiKeys = entities.stream().map(GoogleStudioApiKey::getApiKey).toList();
        List<GoogleStudioApiKey> keys = googleStudioApiKeyRepository.findByApiKeysIn(apiKeys);
        List<String> exists = keys.stream().map(GoogleStudioApiKey::getApiKey).toList();
        // 去掉存在的保存
        entities = entities.stream().filter(e -> !exists.contains(e.getApiKey())).toList();
        return googleStudioApiKeyRepository.saveBatch(entities);
    }

    public void test(IdsListDTO data, String userID) {
        GoogleStudioApiKey googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(data.getId(), userID);
        if (googleStudioApiKey != null) {
            Socks5 socks5 = null;
            if (StringUtils.isEmpty(googleStudioApiKey.getSocks5Id())) {
                socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                if (socks5 == null) {
                    throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                }
                googleStudioApiKey.setSocks5Id(socks5.get_id());
                googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
            } else {
                socks5 = socks5Repository.findSocks5ById(googleStudioApiKey.getSocks5Id());
                if (socks5 == null) {
                    socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                    if (socks5 == null) {
                        throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                    }
                    googleStudioApiKey.setSocks5Id(socks5.get_id());
                    googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
                }
            }
            if (socks5 == null) {
                throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
            }


            String type = AiTypeEnums.GoogleStudio.getCode();
            if (StringUtils.isNotEmpty(googleStudioApiKey.getType())) {
                type = googleStudioApiKey.getType();
            }
            try {
                AiImageRecognition imageRecognition = applicationContext.getBean(type, AiImageRecognition.class);
                if (imageRecognition != null) {
                    for (int i = 0; i < 10; i++) {
                        try {
                            googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(data.getId(), userID);
                            googleStudioApiKey.setUseByMinute(googleStudioApiKey.getUseByMinute() + 1);
                            googleStudioApiKey.setUseByDay(googleStudioApiKey.getUseByDay() + 1);
                            googleStudioApiKeyRepository.update(googleStudioApiKey);
                            break;
                        } catch (Exception e) {
                            if (i == 9) {
                                throw e;
                            }
                        }
                    }
                    boolean validate = imageRecognition.isValidate(socks5, googleStudioApiKey.getApiKey());
                    if (!validate) {
                        googleStudioApiKeyRepository.updateDisable(googleStudioApiKey.get_id());
                    } else {
                        for (int i = 0; i < 10; i++) {
                            try {
                                googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(data.getId(), userID);
                                googleStudioApiKey.setStatus("enable");
                                googleStudioApiKey.setUsedSuccessByDay(googleStudioApiKey.getUsedSuccessByDay() + 1);
                                googleStudioApiKey.setUsedSuccess(googleStudioApiKey.getUsedSuccess() + 1);
                                googleStudioApiKeyRepository.update(googleStudioApiKey);
                                break;
                            } catch (Exception e) {
                                if (i == 9) {
                                    throw e;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                socks5Service.releaseSocks5(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null);
                googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(data.getId(), userID);
                googleStudioApiKey.setSocks5Id("");
                googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
                throw e;
            }
        }
    }

    public void checkTest() {
        List<GoogleStudioApiKey> googleStudioApiKeys = googleStudioApiKeyRepository.findByStatus("enable");
        for (GoogleStudioApiKey googleStudioApiKey : googleStudioApiKeys) {
            try {
                IdsListDTO idsListDTO = new IdsListDTO();
                idsListDTO.setId(googleStudioApiKey.get_id());
                test(idsListDTO, googleStudioApiKey.getUserID());
            } catch (Exception e) {
            }
        }
    }

    public Map<String, Integer> canUseCount(String userID) {
        List<GoogleStudioApiKey> key = googleStudioApiKeyRepository.findByCanUseKey(userID);
        Map<String, Integer> map = new HashMap<>();
        map.put("keyCount", key.size());
        // 默认1000次
        int total = 0;
        int oneKeyCount = 1500;
        int successTotal = 0;
        int usedTotal = 0;
        for (GoogleStudioApiKey googleStudioApiKey : key) {
            total = total + oneKeyCount - googleStudioApiKey.getUseByDay();
            successTotal = successTotal + googleStudioApiKey.getUsedSuccessByDay();
            usedTotal =  usedTotal + googleStudioApiKey.getUseByDay();
        }
        map.put("restCount", total);
        map.put("successCount", successTotal);
        map.put("usedCount", usedTotal);
        return map;
    }
}
