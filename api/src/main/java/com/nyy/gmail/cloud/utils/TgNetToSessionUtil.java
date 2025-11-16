package com.nyy.gmail.cloud.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Slf4j
public class TgNetToSessionUtil {

    public static String BASE_URL = "http://127.0.0.1:18001";

    public static String convert (String tgnet_b64, String user_cfg_b64) {
        try {
            OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(
                    Map.of("tgnet_base64", tgnet_b64, "userconfig_base64", user_cfg_b64));
            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url(BASE_URL + "/api/tgnet/toSession");
            request = builder.post(body).build();
            try (Response response = defaultClient.newCall(request).execute()) {
                if (response.body() != null) {
                    if (response.code() == 200) {
                        String respStr = response.body().string();

                        JSONObject objRes = JSONObject.parseObject(respStr);

                        Integer code = objRes.getInteger("code");
                        if (code != null && code == 0) {
                            return objRes.getJSONObject("data").getString("session");
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public static String fileToBase64(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是一个有效文件: " + filePath);
        }

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
        }
    }

//    public static void main(String[] args) {
//        String file1 = "C:\\Users\\PCV\\Downloads\\_tg_20251014_20\\1bc241ecd21e3a1c\\tgnet.dat";
//        String file2 = "C:\\Users\\PCV\\Downloads\\_tg_20251014_20\\1bc241ecd21e3a1c\\userconfing.xml";
//        String tgbase64 = fileToBase64(file1);
//        String user_cfg_b64 = fileToBase64(file2);
//        String convert = convert(tgbase64, user_cfg_b64);
//        System.out.println(convert);
//    }

//    public static void main(String[] args) {
//        ZipUtil.unzip("C:\\Users\\PCV\\Downloads\\1.txt", "C:\\Users\\PCV\\Downloads\\_tg_20251014_21");
//    }
}
