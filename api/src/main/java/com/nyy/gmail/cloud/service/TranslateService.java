package com.nyy.gmail.cloud.service;

import cn.hutool.json.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.model.dto.TranslateDTO;
import com.nyy.gmail.cloud.model.dto.TranslateResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class TranslateService {

    @Autowired
    @Qualifier("translate")
    private Executor translateExecutor;

    @Resource
    private Socks5Service socks5Service;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public OkHttpClient getHttpClient() {
        return OkHttpClientFactory.getDefaultClient();
    }

    public String doTranslate(TranslateDTO translateDTO, Socks5 socks5) {

        if (StringUtils.isBlank(translateDTO.getContent())) {
            return "";
        }
        try {
            // 设置源语言、目标语言和要翻译的文本
            String sourceLang = translateDTO.getSl(); // 源语言
            String targetLang = translateDTO.getTl(); // 目标语言
            String content = translateDTO.getContent(); // 需要翻译的文本

            // 构建请求的 URL
            String url = genTransUrl(sourceLang, targetLang, content);

            // 创建请求
            Request request = new Request.Builder().url(url).build();

            OkHttpClient httpClient = OkHttpClientFactory.getSocks5Client(socks5);
            // 执行请求
            try (Response response = httpClient.newCall(request).execute()) {
                // 获取响应内容
                String jsonResponse = response.body().string();

                // 解析返回的 JSON 响应
                JSONArray jsonArray = new JSONArray(jsonResponse);
                JSONArray translationArray = jsonArray.getJSONArray(0);
                // 循环translationArray
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < translationArray.size(); i++) {
                    JSONArray innerArray = translationArray.getJSONArray(i);
                    // 提取出translationArray中的第一个元素
                    String translation = innerArray.get(0, String.class);
                    // 说明是输入框中的消息
                    if (!translateDTO.getHistoryMsg()) {
                        // 将 \n 换成 <br/>
                        translation = translation.replace("\n", "<br/>");
                    }// 否则是
                    stringBuilder.append(translation);
                }
                return stringBuilder.toString();
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }

    public TranslateResult translate(TranslateDTO translateDTO) {
        TranslateResult result = new TranslateResult(null, "", translateDTO.getTl());
        if (StringUtils.isBlank(translateDTO.getContent())) {
            return result;
        }

        if (StringUtils.isNotBlank(translateDTO.getId())) {
            // 历史消息 + 历史指定语言
            Object historyResult = redisTemplate.opsForHash().get(translateDTO.getId(), translateDTO.getTl());
            // 命中历史翻译的记录
            if (null != historyResult) {
                result.setId(translateDTO.getId());
                result.setContent(translateDTO.getContent());
                return result;
            }
        }

        // 普通的单条翻译
        Object o = redisTemplate.opsForSet().randomMember(Constants.VALID_IP_LIST_KEY);
        if (o != null) {
            String sock5Str = o.toString();
            Socks5Service.Socks5CacheInfo socks5CacheInfo = socks5Service.parseSocks5Key(sock5Str);
            // 构造一个客户端
            Socks5 socks5 = new Socks5();
            socks5.setIp(socks5CacheInfo.getIp());
            socks5.setPort(socks5CacheInfo.getPort());
            socks5.setPassword(socks5CacheInfo.getPassword());
            socks5.setUsername(socks5CacheInfo.getUsername());
            String content = doTranslate(translateDTO, socks5);
            result = new TranslateResult(translateDTO.getId(), content, translateDTO.getTl());
        } else {
            log.error("translate error translateDTO : {}", JSON.toJSONString(translateDTO));
            return result;
        }
        // 说明是单条翻译
        if (StringUtils.isNotEmpty(translateDTO.getId())) {
            // 历史消息
            redisTemplate.opsForHash().put(translateDTO.getId(), translateDTO.getTl(), result.getContent());
        }
        return result;
    }


    public Map<String, String> translateBatch(List<TranslateDTO> translateDTOList) {
        if (CollectionUtils.isEmpty(translateDTOList)) {
            return new HashMap<>();
        }

        Map<String, String> messageIdTranslateMap = new HashMap<>();
        List<TranslateDTO> needTranslateList = new ArrayList<>();
        for (TranslateDTO translateDTO : translateDTOList) {
            if (StringUtils.isBlank(translateDTO.getId())) {
                continue;
            }
            Object o = redisTemplate.opsForHash().get(translateDTO.getId(), translateDTO.getTl());
            if (null != o) {
                messageIdTranslateMap.put(translateDTO.getId(), o.toString());
            } else {
                needTranslateList.add(translateDTO);
            }
        }

        // 需要翻译的消息
        List<CompletableFuture<TranslateResult>> futures = needTranslateList.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> translate(item), translateExecutor))
                .toList();

        /*归并结果*/
        List<TranslateResult> results = futures.stream().filter(future -> null != future.join()).map
                (CompletableFuture::join).toList();

        // 将results转换成map TranslateResult.getId, TranslateResult.getContent
        for (TranslateResult translateResult : results) {
            messageIdTranslateMap.put(translateResult.getId(), translateResult.getContent());
            if (StringUtils.isNotBlank(translateResult.getContent())) {
                redisTemplate.opsForHash().put(translateResult.getId(), translateResult.getTl(), translateResult.getContent());
            }
        }

        return messageIdTranslateMap;
    }


    private String genTransUrl(String sourceLang, String targetLang, String content) {
        String url;
        try {
            url = String.format(
                    "https://translate.googleapis.com/translate_a/single?client=gtx&dt=t&sl=%s&tl=%s&q=%s",
                    sourceLang,
                    targetLang,
                    URLEncoder.encode(content, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

}
