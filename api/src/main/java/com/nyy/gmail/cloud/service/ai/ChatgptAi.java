package com.nyy.gmail.cloud.service.ai;

import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.utils.ChatgptAiUtils;
import com.nyy.gmail.cloud.utils.GoogleGenAiUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("Chatgpt")
public class ChatgptAi implements AiImageRecognition {

    @Value("${gateway.aiChatgpt}")
    private String aiChatGptKey;

    @Override
    public List<List<GoogleGenAiUtils.Result>> imageRecognition(Socks5 socks5, List<String> filepaths, String apiKey, String promptVersion, String model, String mimeType) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public List<List<GoogleGenAiUtils.Result>> imageRecognition(Socks5 socks5, List<String> filepaths, String apiKey, String promptVersion) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public boolean isValidate(Socks5 socks5, String apiKey) {
        try {
            ChatgptAiUtils.normalChat(socks5, apiKey, "hi", "");
            return true;
        } catch (Exception e) {
            if (e instanceof CommonException && e.getMessage().contains("401")) {
               return false;
            }
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String emailContentOptimize(Socks5 socks5, String content, String apiKey) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public String emailContentOptimizeV2(String content, String apiKey, int n) throws Exception {
        return ChatgptAiUtils.emailContentOptimizeV2(content, aiChatGptKey, n);
    }

    @Override
    public String emailTitleOptimizeV2(String title, String apiKey, int n) throws Exception {
        return ChatgptAiUtils.emailTitleOptimizeV2(title, aiChatGptKey, n);
    }

    @Override
    public String emailContentGenerate(String thisContent, String apiKey, int n) throws Exception {
        return ChatgptAiUtils.emailContentGenerate(thisContent, aiChatGptKey, null, n);
    }

    @Override
    public String proxyMode(Socks5 socks5, String apiKey, String jsonBody, String model) throws Exception {
        return ChatgptAiUtils.proxyMode(socks5, apiKey, jsonBody);
    }

    @Override
    public String emailContentGenerateV2(String thisContent, String subject, Map<String, String> param, String apiKey, String prompt, int n) throws Exception {
        return ChatgptAiUtils.emailContentGenerateV2(thisContent, subject, param, aiChatGptKey, prompt, n);
    }

}
