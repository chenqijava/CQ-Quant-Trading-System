package com.nyy.gmail.cloud.jobs;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.GoogleAiServer;
import com.nyy.gmail.cloud.repository.mongo.GoogleAiServerRepository;
import com.nyy.gmail.cloud.service.GoogleAiServerService;
import com.nyy.gmail.cloud.utils.SignGenerator;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CheckAiServerScoreJob {

    @Autowired
    private GoogleAiServerRepository googleAiServerRepository;

    @Autowired
    private GoogleAiServerService googleAiServerService;

    @Async("other")
    @Scheduled(cron = "0 * * * * ?")
    public void run () {
        googleAiServerService.uploadSever();
    }
}
