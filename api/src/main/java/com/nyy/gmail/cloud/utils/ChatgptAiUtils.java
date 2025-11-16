package com.nyy.gmail.cloud.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChatgptAiUtils {

    public static String BASE_URL = "http://localhost:9000/newchat2";

    public static String emailContentOptimizeV2(String content, String apiKey, int n) throws Exception {
        return emailContentOptimizeV2(content, apiKey, null, n);
    }

    public static String emailContentOptimizeV2(String content, String apiKey, String prompt, int n) throws Exception {
        String model = "auto";
        Date start = new Date();
        log.info(apiKey + " 开始邮件改写," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            prompt = (StringUtils.isEmpty(prompt) ? GoogleGenAiUtils.emailOptimizePromptList.get((int) Math.floor(Math.random() * GoogleGenAiUtils.emailOptimizePromptList.size())) : prompt).replace("$Content", content).replace("$UUID", UUIDUtils.get32UUId()).replace("$N", String.valueOf(n));
            log.info(prompt);

            OkHttpClient googleAi = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("model", model, "messages", List.of(Map.of("role", "user", "content", prompt))));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://gmail10.tnt-pub.com/api/open/v1beta/v1/chat/completions");
            request = builder.post(body).addHeader("Authorization", "Bearer " + apiKey).build();

            log.info(apiKey + " 邮件改写 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.code() == 429) {
                    Thread.sleep(3000);
                }
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(apiKey + " 邮件改写,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                    log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info(apiKey + " " + "result: " + respStr.replace("\n", ""));
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    respStr = objRes.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    return respStr;
                }
            }
            throw new Exception("邮件改写失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(apiKey + " 结束邮件改写,失败: {}", e.getMessage());
            log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }

    public static String emailTitleOptimizeV2(String content, String apiKey, int n) throws Exception {
        return emailTitleOptimizeV2(content, apiKey, null, n);
    }

    public static String emailTitleOptimizeV2(String title, String apiKey, String prompt, int n) throws Exception {
        String model = "auto";
        Date start = new Date();
        log.info(apiKey + " 开始邮件改写," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            prompt = (StringUtils.isEmpty(prompt) ? GoogleGenAiUtils.emailOptimizeTitlePromptList.get((int) Math.floor(Math.random() * GoogleGenAiUtils.emailOptimizeTitlePromptList.size())) : prompt).replace("$Content", title).replace("$UUID", UUIDUtils.get32UUId()).replace("$N", String.valueOf(n));
            log.info(prompt);

            List<Map<String, Object>> parts = new ArrayList();
            parts.add(Map.of("text", prompt));

            OkHttpClient googleAi = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("model", model, "messages", List.of(Map.of("role", "user", "content", prompt))));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://gmail10.tnt-pub.com/api/open/v1beta/v1/chat/completions");
            request = builder.post(body).addHeader("Authorization", "Bearer " + apiKey).build();

            log.info(apiKey + " 邮件改写 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.code() == 429) {
                    Thread.sleep(3000);
                }
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(apiKey + " 邮件改写,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                    log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info(apiKey + " " + "result: " + respStr.replace("\n", ""));
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    respStr = objRes.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    return respStr;
                }
            }
            throw new Exception("邮件改写失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(apiKey + " 结束邮件改写,失败: {}", e.getMessage());
            log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }

    public static String normalChat (Socks5 socks5,String apiKey, String prompt, String model) throws Exception {
        String endApiKey = apiKey.substring(apiKey.length() - 20);
        if (StringUtils.isEmpty(model)) {
            model = "auto";
        }
        Date start = new Date();
        String proxy = socks5.getIp() + ";" + socks5.getPort() + ";" + socks5.getUsername() + ";" + socks5.getPassword();
        try {
            OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("model", model, "messages", List.of(Map.of("role", "user", "content", prompt)), "stream", false,
                    "proxy", "socks5://" + socks5.getUsername() + ":" + socks5.getPassword() + "@" + socks5.getIp() + ":" + socks5.getPort()));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url(BASE_URL + "/v1/chat/completions");
            request = builder.post(body).addHeader("Authorization", "Bearer " + apiKey).build();
            try (Response response = defaultClient.newCall(request).execute()) {
                if (response.body() != null) {
                    if (response.code() == 200) {
                        String respStr = response.body().string();
                        Date end = new Date();
                        log.info(endApiKey + " success spend: " + (end.getTime() - start.getTime()) + "ms");

                        JSONObject objRes = JSONObject.parseObject(respStr);

                        JSONArray choices = objRes.getJSONArray("choices");
                        if (choices != null && !choices.isEmpty()) {
                            return choices.getJSONObject(0).getJSONObject("message").getString("content");
                        } else {
                            log.info(endApiKey + " fail spend. Resp: " + respStr);
                        }
                    } else {
                        throw new CommonException(ResultCode.CHATGPT_AI_ERROR, response.code() + ": " + response.body().string());
                    }
                }
            }
            throw new Exception("发送失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(proxy + endApiKey + " failed spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }

    public static String proxyMode (Socks5 socks5,String apiKey, String jsonBody) throws Exception {
        OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
        MediaType mediaType = MediaType.parse("application/json");

        Map params = JSON.parseObject(jsonBody,Map.class);
        params.put("stream", false);
        params.put("proxy", "socks5://" + socks5.getUsername() + ":" + socks5.getPassword() + "@" + socks5.getIp() + ":" + socks5.getPort());
        String jsonBody2 = JSON.toJSONString(params);

        RequestBody body = RequestBody.create(jsonBody2, mediaType);

        Request request = null;
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/v1/chat/completions");
        request = builder.post(body).addHeader("Authorization", "Bearer " + apiKey).build();
        try (Response response = defaultClient.newCall(request).execute()) {
            if (response.body() != null) {
                String respStr = response.body().string();

                return JSON.toJSONString(Map.of("code", response.code() + "", "respStr", respStr));
            }
        }
        throw new Exception("调用失败");
    }

    public static void main(String[] args) throws Exception {
        Socks5 socks5 = new Socks5();
        socks5.setIp("15.228.185.144");
        socks5.setPort(10240);
        socks5.setUsername("eric");
        socks5.setPassword("ericss10238");

        String token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE5MzQ0ZTY1LWJiYzktNDRkMS1hOWQwLWY5NTdiMDc5YmQwZSIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS92MSJdLCJjbGllbnRfaWQiOiJhcHBfWDh6WTZ2VzJwUTl0UjNkRTduSzFqTDVnSCIsImV4cCI6MTc2MTA0MDI3OCwiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS9hdXRoIjp7ImNoYXRncHRfY29tcHV0ZV9yZXNpZGVuY3kiOiJub19jb25zdHJhaW50IiwiY2hhdGdwdF9kYXRhX3Jlc2lkZW5jeSI6Im5vX2NvbnN0cmFpbnQiLCJ1c2VyX2lkIjoidXNlci01WWw1MnlCWFI2N2ZJYUlrQUIxQ1ZpeHEifSwiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS9wcm9maWxlIjp7ImVtYWlsIjoicnRlcmFtYXNobGV5QGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlfSwiaWF0IjoxNzYwMTc2Mjc4LCJpc3MiOiJodHRwczovL2F1dGgub3BlbmFpLmNvbSIsImp0aSI6ImNjMjVkZjRhLTcxNjgtNDVmMS05ZThhLWVkMmQxNjZmM2E3MiIsIm5iZiI6MTc2MDE3NjI3OCwicHdkX2F1dGhfdGltZSI6MTc2MDE3NjI3NTcxOCwic2NwIjpbIm9wZW5pZCIsImVtYWlsIiwicHJvZmlsZSIsIm9mZmxpbmVfYWNjZXNzIiwibW9kZWwucmVxdWVzdCIsIm1vZGVsLnJlYWQiLCJvcmdhbml6YXRpb24ucmVhZCIsIm9yZ2FuaXphdGlvbi53cml0ZSJdLCJzZXNzaW9uX2lkIjoiYXV0aHNlc3NfdE80UUhZd2Joc2ppOFZWV1l6dHR2VkhVIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMDM0MDAyNTU5OTg3NjAwNjkyMjUifQ.ZvSUn9usU5Q65CZvkOgnbZPfqUynG3uDyrxkr9BHtrAZ00kn8uO9ifKLrbhW2H6dB0gqAYRz-G7mc81O5PTat7StAoDQGp2Mpjj6DTwiThcBmgtCe2iFn0AYdlqwSPOxuzYWXf-QDOHKdUABjtktGRnG8qzJU5GjJtuckIpfAPjoKjP1AC9241XCJIlxwfewAbdQ7GZf1t2eu-v9Hb8Rq8SdkozgIokV5-rRMUm-SY0JGddsoNKmuQLlvq1Vw3ZbKX0-f22yKeo_lrxWn8z8IN7seQXGfmO56P-IuEGEGtmzw5HC5TyfztDxN05db0PP5QtQ-KtbKrg7G-8k6rAeUO9lijH0RdfCcWb8d84Y9fWljGCGc_ARq9FGGAHYQSnhliDr98Lx-IcT1vOqUba1KQjIjxfH-lHHcwmVFUCp12uFG9S5xcbey3baYhN-Ib5u4qEPdkPtaDlZtqh4sNH2UrP5qiSikVy1SqyqS4vmi94-HjrCgH5Hzw3_FoNaZO_hFFbEo35wwZL5iLsgSTLYaqTu9_8jS-C5dzPLmvlAmk4qsD_Nl9rZ4YRJsnqJZ1LtEQ1zMB4cSBYHadT68Yg7GCQSVHfEy_kXy4yAZlNelNZz8Zj6gDjaUBbh2Jdyt7T2zLaQOvBimSSyL35uqia_AF5lKN_oFrHH-KIhMpvB3NY";

        String prompt = "你好，请简单介绍一下你自己";
        // 没有：gpt-3.5-turbo/gpt-3.5-turbo-16k/gpt-4/gpt-4-turbo
        // gpt-5\gpt-4o\gpt-4o-mini\
        // gpt-5/gpt-4o-mini
//        String s = normalChat(socks5, token, prompt, "gpt-4o-mini");
        String resp = proxyMode(socks5, token, JSON.toJSONString(Map.of("model", "auto", "messages", List.of(Map.of("role", "user", "content", "介绍一下你自己")))));

        System.out.println(resp);

    }

    public static String emailContentGenerate(String content, String apiKey, String prompt, int n) throws Exception {
        String model = "auto";
        Date start = new Date();
        log.info(apiKey + " 开始邮件改写," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            prompt = (StringUtils.isEmpty(prompt) ? GoogleGenAiUtils.emailGeneratePromptList.get((int) Math.floor(Math.random() * GoogleGenAiUtils.emailGeneratePromptList.size())) : prompt).replace("$Content", content).replace("$UUID", UUIDUtils.get32UUId()).replace("$N", String.valueOf(n));
            log.info(prompt);

            OkHttpClient googleAi = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("model", model, "messages", List.of(Map.of("role", "user", "content", prompt))));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://gmail10.tnt-pub.com/api/open/v1beta/v1/chat/completions");
            request = builder.post(body).addHeader("Authorization", "Bearer " + apiKey).build();

            log.info(apiKey + " 邮件改写 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.code() == 429) {
                    Thread.sleep(3000);
                }
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(apiKey + " 邮件改写,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                    log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info(apiKey + " " + "result: " + respStr.replace("\n", ""));
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    respStr = objRes.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    return respStr;
                }
            }
            throw new Exception("邮件改写失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(apiKey + " 结束邮件改写,失败: {}", e.getMessage());
            log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }

    public static String emailContentGenerateV2(String content, String subject, Map<String, String> param, String apiKey, String prompt, int n) throws Exception {
        String model = "auto";
        Date start = new Date();
        log.info(apiKey + " 开始邮件改写," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            prompt = (StringUtils.isEmpty(prompt) ? GoogleGenAiUtils.emailGeneratePromptListV3.get((int) Math.floor(Math.random() * GoogleGenAiUtils.emailGeneratePromptListV3.size())) : prompt)
                    .replace("$Content", content).replace("$Subject", subject).replace("$UUID", UUIDUtils.get32UUId()).replace("$N", String.valueOf(n));
            for (String key: param.keySet()) {
                prompt = prompt.replace("$" + key, param.get(key));
            }
            log.info(prompt);

            OkHttpClient googleAi = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("model", model, "messages", List.of(Map.of("role", "user", "content", prompt))));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://gmail10.tnt-pub.com/api/open/v1beta/v1/chat/completions");
            request = builder.post(body).addHeader("Authorization", "Bearer " + apiKey).build();

            log.info(apiKey + " 邮件改写 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.code() == 429) {
                    Thread.sleep(3000);
                }
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(apiKey + " 邮件改写,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                    log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info(apiKey + " " + "result: " + respStr.replace("\n", ""));
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    respStr = objRes.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    return respStr;
                }
            }
            throw new Exception("邮件改写失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(apiKey + " 结束邮件改写,失败: {}", e.getMessage());
            log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }
}
