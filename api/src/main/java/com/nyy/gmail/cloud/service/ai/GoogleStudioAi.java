package com.nyy.gmail.cloud.service.ai;

import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.entity.mongo.GoogleStudioApiKey;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.utils.ChatgptAiUtils;
import com.nyy.gmail.cloud.utils.GoogleGenAiUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("GoogleStudio")
public class GoogleStudioAi implements AiImageRecognition {

    @Value("${gateway.aiGoogleKey}")
    private String aiGoogleKey;

    @Override
    public List<List<GoogleGenAiUtils.Result>> imageRecognition(Socks5 socks5, List<String> filepaths, String apiKey, String promptVersion, String model, String mimeType) throws Exception {
        return GoogleGenAiUtils.imageRecognition(socks5, filepaths, apiKey, promptVersion, model, mimeType,null);
    }

    @Override
    public List<List<GoogleGenAiUtils.Result>> imageRecognition(Socks5 socks5, List<String> filepaths, String apiKey, String promptVersion) throws Exception {
        return GoogleGenAiUtils.imageRecognition(socks5, filepaths, apiKey, promptVersion);
    }

    @Override
    public boolean isValidate(Socks5 socks5, String apiKey) {
        try {
            GoogleGenAiUtils.normalChat(socks5, apiKey, "hi", "gemini-2.5-flash-lite");
            return true;
        } catch (Exception e) {
            if (e instanceof CommonException && e.getMessage().contains("403")) {
               return false;
            }
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String emailContentOptimize(Socks5 socks5, String content, String apiKey) throws Exception {
        return GoogleGenAiUtils.emailContentOptimize(socks5, content, apiKey);
    }

    @Override
    public String emailContentOptimizeV2(String content, String apiKey, int n) throws Exception {
        return GoogleGenAiUtils.emailContentOptimizeV2(content, aiGoogleKey, n);
    }

    @Override
    public String emailTitleOptimizeV2(String title, String apiKey, int n) throws Exception {
        return GoogleGenAiUtils.emailTitleOptimizeV2(title, aiGoogleKey, n);
    }

    @Override
    public String emailContentGenerate(String thisContent, String apiKey, int n) throws Exception {
        return GoogleGenAiUtils.emailContentGenerate(thisContent, aiGoogleKey, null, n);
    }

    @Override
    public String proxyMode(Socks5 socks5, String apiKey, String jsonBody, String model) throws Exception {
        return GoogleGenAiUtils.proxyMode(socks5, apiKey, jsonBody, model);
    }

    @Override
    public String emailContentGenerateV2(String thisContent, String subject, Map<String, String> param, String apiKey, String prompt, int n) throws Exception {
        return GoogleGenAiUtils.emailContentGenerateV2(thisContent, subject, param, apiKey, prompt, n);
    }

}
