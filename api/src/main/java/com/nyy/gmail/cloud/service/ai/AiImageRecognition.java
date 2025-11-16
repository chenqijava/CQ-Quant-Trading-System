package com.nyy.gmail.cloud.service.ai;

import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.utils.GoogleGenAiUtils;

import java.util.List;
import java.util.Map;

public interface AiImageRecognition {
    List<List<GoogleGenAiUtils.Result>> imageRecognition (Socks5 socks5, List<String> filepaths, String apiKey, String promptVersion, String model, String mimeType) throws Exception;

    List<List<GoogleGenAiUtils.Result>> imageRecognition (Socks5 socks5, List<String> filepaths, String apiKey, String promptVersion) throws Exception;

    boolean isValidate(Socks5 socks5, String apiKey);

    String emailContentOptimize(Socks5 socks5, String content, String apiKey) throws Exception;

    String proxyMode(Socks5 socks5, String apiKey, String jsonBody, String model) throws Exception;

    String emailContentOptimizeV2(String thisContent, String apiKey, int n)  throws Exception;

    String emailTitleOptimizeV2(String title, String apiKey, int n) throws Exception;

    String emailContentGenerate(String thisContent, String apiKey, int n)  throws Exception;

    String emailContentGenerateV2(String thisContent, String subject, Map<String, String> param, String apiKey, String prompt, int n)  throws Exception;

}
