package com.nyy.gmail.cloud.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.UserApiKeyGeminiCallInfo;
import com.nyy.gmail.cloud.entity.mongo.UserApiKeyTokenStatistics;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.repository.mongo.UserApiKeyGeminiCallInfoRepository;
import com.nyy.gmail.cloud.repository.mongo.UserApiKeyTokenStatisticsRepository;
import com.nyy.gmail.cloud.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserApiKeyTokenStatisticsService {

    @Autowired
    private UserApiKeyTokenStatisticsRepository userApiKeyTokenStatisticsRepository;

    @Autowired
    private UserApiKeyGeminiCallInfoRepository userApiKeyGeminiCallInfoRepository;

    public PageResult<UserApiKeyTokenStatistics> findByPagination(Params params, int pageSize, int page, String userID) {
        if (params.getFilters() != null && params.getFilters().containsKey("createTimeRange")) {
            Date startDate = null;
            Date endDate = null;
            List<String> createTimeRange = (List<String>)params.getFilters().get("createTimeRange");
            params.getFilters().remove("createTimeRange");
            if (createTimeRange.size() == 2) {
                startDate = Objects.requireNonNull(DateUtil.getDateByFormat(createTimeRange.getFirst(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                endDate = Objects.requireNonNull(DateUtil.getDateByFormat(createTimeRange.getLast(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            }
            PageResult<UserApiKeyTokenStatistics> result = userApiKeyTokenStatisticsRepository.findOneByUserApiKey(params, pageSize, page);

            List<String> collect = result.getData().stream().map(UserApiKeyTokenStatistics::getApiKey).collect(Collectors.toList());
            List<UserApiKeyGeminiCallInfo> userApiKeyGeminiCallInfos = userApiKeyGeminiCallInfoRepository.findAll(collect, startDate, endDate);

            Map<String, List<UserApiKeyGeminiCallInfo>> groupedMap = userApiKeyGeminiCallInfos.stream()
                    .collect(Collectors.groupingBy(UserApiKeyGeminiCallInfo::getUserKeyApi));

            for (UserApiKeyTokenStatistics  userApiKeyTokenStatistics : result.getData()) {
                userApiKeyTokenStatistics.setCallCount(0L);
                userApiKeyTokenStatistics.setPromptTokenCount(0L);
                userApiKeyTokenStatistics.setCandidatesTokenCount(0L);
                userApiKeyTokenStatistics.setOutputTokenCount(0L);
                userApiKeyTokenStatistics.setTotalTokenCount(0L);

                if (groupedMap.containsKey(userApiKeyTokenStatistics.getApiKey())) {
                    for (UserApiKeyGeminiCallInfo userApiKeyGeminiCallInfo : groupedMap.get(userApiKeyTokenStatistics.getApiKey())) {
                        try {
                            JSONObject jsonObject = JSON.parseObject(userApiKeyGeminiCallInfo.getResponseTxt());
                            JSONObject metadata = jsonObject.getJSONObject("usageMetadata");
                            Long promptTokenCount = metadata.getLong("promptTokenCount");
                            Long candidatesTokenCount = metadata.getLong("candidatesTokenCount");
                            Long totalTokenCount = metadata.getLong("totalTokenCount");
                            userApiKeyTokenStatistics.setCallCount(userApiKeyTokenStatistics.getCallCount() + 1);
                            userApiKeyTokenStatistics.setPromptTokenCount(promptTokenCount + userApiKeyTokenStatistics.getPromptTokenCount());
                            userApiKeyTokenStatistics.setCandidatesTokenCount(candidatesTokenCount + userApiKeyTokenStatistics.getCandidatesTokenCount());
                            userApiKeyTokenStatistics.setTotalTokenCount(totalTokenCount + userApiKeyTokenStatistics.getTotalTokenCount());
                            userApiKeyTokenStatistics.setOutputTokenCount(userApiKeyTokenStatistics.getOutputTokenCount() + totalTokenCount - promptTokenCount);
                        } catch (Exception e) {
                        }
                        try {
                            JSONObject jsonObject = JSON.parseObject(userApiKeyGeminiCallInfo.getResponseTxt());
                            JSONObject metadata = jsonObject.getJSONObject("usage");
                            Long promptTokenCount = metadata.getLong("prompt_tokens");
                            Long candidatesTokenCount = metadata.getLong("completion_tokens");
                            Long totalTokenCount = metadata.getLong("total_tokens");
                            userApiKeyTokenStatistics.setCallCount(userApiKeyTokenStatistics.getCallCount() + 1);
                            userApiKeyTokenStatistics.setPromptTokenCount(promptTokenCount + userApiKeyTokenStatistics.getPromptTokenCount());
                            userApiKeyTokenStatistics.setCandidatesTokenCount(candidatesTokenCount + userApiKeyTokenStatistics.getCandidatesTokenCount());
                            userApiKeyTokenStatistics.setTotalTokenCount(totalTokenCount + userApiKeyTokenStatistics.getTotalTokenCount());
                            userApiKeyTokenStatistics.setOutputTokenCount(userApiKeyTokenStatistics.getOutputTokenCount() + totalTokenCount - promptTokenCount);
                        } catch (Exception e) {
                        }
                    }
                }
            }
            return result;
        }
        return userApiKeyTokenStatisticsRepository.findOneByUserApiKey(params, pageSize, page);
    }
}
