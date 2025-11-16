package com.nyy.gmail.cloud.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;

@Slf4j
public class HttpUtil {
    public static String cutBefore(String input, String marker) {
        if (input == null || marker == null) {
            return input;
        }

        int index = input.indexOf(marker);
        if (index == -1) {
            return input; // 没找到A，原样返回
        }

        // 从A之后的部分
        String afterA = input.substring(index + marker.length());

        // 如果A后面不包含 } 或 ]
        if (!afterA.contains("}") && !afterA.contains("]")) {
            // 截取A前面的内容
            return input.substring(0, index);
        }

        // 否则不改动
        return input;
    }

    public static String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = OkHttpClientFactory.getDefaultClient().newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body().string();
        }
        return null;
    }

    public static JSONObject postJson(String url, JSONObject jsonBody) throws IOException {
        RequestBody requestBody = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json")
        );

        OkHttpClient client = OkHttpClientFactory.getDefaultClient();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
            String responseBody = response.body().string();
            return JSON.parseObject(responseBody);
        } else {
            return null;
//            throw new RuntimeException("Bad request:" + response.code());
        }
    }
}
