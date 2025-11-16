package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.AiStatistics;
import com.nyy.gmail.cloud.entity.mongo.GoogleStudioApiKey;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.enums.AiTypeEnums;
import com.nyy.gmail.cloud.repository.mongo.AiStatisticsRepository;
import com.nyy.gmail.cloud.repository.mongo.GoogleStudioApiKeyRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class AiStatisticsService {

    @Autowired
    private AiStatisticsRepository aiStatisticsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoogleStudioApiKeyRepository googleStudioApiKeyRepository;

    public void statistics() {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 0);
        Date time = instance.getTime();

        List<User> all = userRepository.findAll();
        for (User user : all) {
            List<GoogleStudioApiKey> googleStudioApiKeyList = googleStudioApiKeyRepository.findByUserID(user.getUserID());
            long totalCallNum = 0;
            long totalSuccessNum = 0;
            long totalAccountNum = 0;
            for (AiTypeEnums typeEnums : AiTypeEnums.values()) {
                long callNum = 0;
                long successNum = 0;
                long accountNum = 0;
                for (GoogleStudioApiKey googleStudioApiKey : googleStudioApiKeyList) {
                    if (googleStudioApiKey.getType() != null && googleStudioApiKey.getType().equals(typeEnums.getCode())) {
                        callNum += googleStudioApiKey.getUseByDay();
                        successNum += googleStudioApiKey.getUsedSuccessByDay();
                        accountNum++;
                    }
                    if (StringUtils.isEmpty(googleStudioApiKey.getType()) && typeEnums.getCode().equals(AiTypeEnums.GoogleStudio.getCode())) {
                        callNum += googleStudioApiKey.getUseByDay();
                        successNum += googleStudioApiKey.getUsedSuccessByDay();
                        accountNum++;
                    }
                }
                AiStatistics aiStatistics = aiStatisticsRepository.findLastByTypeAndUserID(typeEnums.getCode(), user.getUserID());
                if (aiStatistics == null) {
                    aiStatistics = new AiStatistics();
                    aiStatistics.setType(typeEnums.getCode());
                    aiStatistics.setCallNum(callNum);
                    aiStatistics.setCreateTime(time);
                    aiStatistics.setUserID(user.getUserID());
                    aiStatistics.setSuccessNum(successNum);
                    aiStatistics.setAccountNum(accountNum);

                    aiStatisticsRepository.save(aiStatistics);
                } else {
                    aiStatistics.setCallNum(callNum);
                    aiStatistics.setSuccessNum(successNum);
                    aiStatistics.setAccountNum(accountNum);

                    aiStatisticsRepository.update(aiStatistics);
                }

                totalCallNum += callNum;
                totalSuccessNum += successNum;
                totalAccountNum += accountNum;
            }

            AiStatistics aiStatistics = aiStatisticsRepository.findLastByTypeAndUserID("total", user.getUserID());

            if (aiStatistics == null) {
                aiStatistics = new AiStatistics();
                aiStatistics.setType("total");
                aiStatistics.setCallNum(totalCallNum);
                aiStatistics.setCreateTime(time);
                aiStatistics.setSuccessNum(totalSuccessNum);
                aiStatistics.setUserID(user.getUserID());
                aiStatistics.setAccountNum(totalAccountNum);

                aiStatisticsRepository.save(aiStatistics);
            } else {
                aiStatistics.setCallNum(totalCallNum);
                aiStatistics.setSuccessNum(totalSuccessNum);
                aiStatistics.setAccountNum(totalAccountNum);

                aiStatisticsRepository.update(aiStatistics);
            }

        }
    }

    public void newStatistics() {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 0);
        Date time = instance.getTime();

        List<User> all = userRepository.findAll();
        for (User user : all) {
            List<GoogleStudioApiKey> googleStudioApiKeyList = googleStudioApiKeyRepository.findByUserID(user.getUserID());
            long totalCallNum = 0;
            long totalSuccessNum = 0;
            long totalAccountNum = 0;
            for (AiTypeEnums typeEnums : AiTypeEnums.values()) {
                long callNum = 0;
                long successNum = 0;
                long accountNum = 0;
                for (GoogleStudioApiKey googleStudioApiKey : googleStudioApiKeyList) {
                    if (googleStudioApiKey.getType() != null && googleStudioApiKey.getType().equals(typeEnums.getCode())) {
                        callNum += googleStudioApiKey.getUseByDay();
                        successNum += googleStudioApiKey.getUsedSuccessByDay();
                        accountNum ++;
                    }
                    if (StringUtils.isEmpty(googleStudioApiKey.getType()) && typeEnums.getCode().equals(AiTypeEnums.GoogleStudio.getCode())) {
                        callNum += googleStudioApiKey.getUseByDay();
                        successNum += googleStudioApiKey.getUsedSuccessByDay();
                        accountNum ++;
                    }
                }
                AiStatistics aiStatistics = new AiStatistics();
                aiStatistics.setType(typeEnums.getCode());
                aiStatistics.setCallNum(callNum);
                aiStatistics.setCreateTime(time);
                aiStatistics.setUserID(user.getUserID());
                aiStatistics.setSuccessNum(successNum);
                aiStatistics.setAccountNum(accountNum);

                aiStatisticsRepository.save(aiStatistics);
                totalCallNum += callNum;
                totalSuccessNum += successNum;
                totalAccountNum += accountNum;
            }
            AiStatistics aiStatistics = new AiStatistics();
            aiStatistics.setType("total");
            aiStatistics.setCallNum(totalCallNum);
            aiStatistics.setCreateTime(time);
            aiStatistics.setSuccessNum(totalSuccessNum);
            aiStatistics.setUserID(user.getUserID());
            aiStatistics.setAccountNum(totalAccountNum);

            aiStatisticsRepository.save(aiStatistics);
        }
    }

    public List<AiStatistics> findList(String userID, int pageSize, int page) {
        return aiStatisticsRepository.findList(userID, pageSize, page);
    }
}
