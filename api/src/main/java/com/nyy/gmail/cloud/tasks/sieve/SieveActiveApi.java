package com.nyy.gmail.cloud.tasks.sieve;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.model.bo.UploadFileBO;
import com.nyy.gmail.cloud.service.ParamsService;
import com.nyy.gmail.cloud.tasks.sieve.dto.DownloadTaskDTO;
import com.nyy.gmail.cloud.tasks.sieve.dto.OpTaskDTO;
import com.nyy.gmail.cloud.tasks.sieve.dto.QueryTaskResponseDTO;
import com.nyy.gmail.cloud.tasks.sieve.dto.SendTaskDTO;
import com.nyy.gmail.cloud.tasks.sieve.enums.SieveActiveTaskResultTypeEnum;
import com.nyy.gmail.cloud.utils.SieveActiveTaskUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SieveActiveApi {
    public static final HttpLoggingInterceptor basicLoggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC);
    public static final HttpLoggingInterceptor bodyLoggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);

    private final static OkHttpClient defaultClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build();

    @Autowired
    private ParamsService paramsService;

    public UploadFileBO uploadFile(File file) throws Exception {
        String url = getServerHost() + "/api/consumer/res/upload/dataFile";
        long startTime = System.currentTimeMillis();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse("application/octet-stream")))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "gmail-cloud")
                .addHeader("Cookie", getServerCookie())
                .post(requestBody)
                .build();

        try (Response response = getUploadClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("请求总控上传接口失败,url:【{}】,状态码:【{}】", url, response.code());
                throw new Exception("请求总控上传接口失败,url: " + url + ",状态码:" + response.code());
            }
            String responseBody = response.body().string();
            log.info("请求总控上传接口:【{}】,参数:【{}】,结果:【{}】,cost:【{}】ms", url, file.getName(), responseBody, System.currentTimeMillis() - startTime);
            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            if (jsonObject.getInteger("code") == 1) {
                return  JSONObject.parseObject(jsonObject.getString("data"), UploadFileBO.class);
            }
        }
        return null;
    }

    public String postTask(SendTaskDTO sendTaskDTO) throws Exception {
        String url = getServerHost() + "/api/consumer/orderTask/postTask";
        String body = JSONObject.toJSONString(sendTaskDTO);
        long startTime = System.currentTimeMillis();

        Request request = getRequest(url, body);
        try (Response response = getDefaultClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("请求提交任务接口失败,url: " + url + ",状态码:" + response.code());
            }
            String responseBody = response.body().string();
            log.info("请求提交任务接口:【{}】,参数:【{}】,结果:【{}】,cost:【{}】ms", url, body, responseBody, System.currentTimeMillis() - startTime);
            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            if (jsonObject.getInteger("code") == 1) {
                return jsonObject.getString("data");
            }
        }
        return null;
    }

    public QueryTaskResponseDTO query(OpTaskDTO opTaskDTO) throws Exception {
        String groupTaskId = opTaskDTO.getGroupTaskId();
        String url = String.format(getServerHost() + "/api/consumer/orderTask/query/%s", groupTaskId);
        long startTime = System.currentTimeMillis();
        Headers headers = new Headers.Builder()
                .add("User-Agent", "gmail-cloud")
                .add("Cookie", getServerCookie())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .get()
                .build();
        try (Response response = getDefaultClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("请求查询接口失败,url: " + url + ",状态码:" + response.code());
            }
            String responseBody = response.body().string();
            log.info("请求查询接口:【{}】,参数:【{}】,结果:【{}】,cost:【{}】ms", url, groupTaskId, responseBody, System.currentTimeMillis() - startTime);
            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            if (jsonObject.getInteger("code") == 1) {
                return JSONObject.parseObject(jsonObject.getString("data"), QueryTaskResponseDTO.class);
            }
        }
        return null;
    }

    public boolean stop(OpTaskDTO opTaskDTO) throws Exception {
        String groupTaskId = opTaskDTO.getGroupTaskId();
        String url = String.format(getServerHost() + "/api/consumer/orderTask/stop");
        JSONObject json = new JSONObject();
        json.put("ids", Collections.singletonList(groupTaskId));
        json.put("reason", opTaskDTO.getReason());
        long startTime = System.currentTimeMillis();
        Request request = getRequest(url, json.toJSONString());
        try (Response response = getDefaultClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("请求结束任务接口失败,url: " + url + ",状态码:" + response.code());
            }
            String responseBody = response.body().string();
            log.info("请求结束任务接口:【{}】,参数:【{}】,结果:【{}】,cost:【{}】ms", url, json, responseBody, System.currentTimeMillis() - startTime);
            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            if (jsonObject.getInteger("code") == 1) {
                return true;
            }
        }
        return false;
    }

    public File downloadFile(DownloadTaskDTO downloadTaskDTO) throws Exception {
        SieveActiveTaskResultTypeEnum type = downloadTaskDTO.getType();
        String groupTaskId = downloadTaskDTO.getGroupTaskId();
        Path parentPath = downloadTaskDTO.getParentPath();
        String filepath = downloadTaskDTO.getFilepath();
        return switch (type) {
            case success, failed, unexecute, unknown, forbidden -> download(groupTaskId, parentPath, filepath);
            default -> null;
        };
    }

    public File download(String groupTaskId, Path parentPath, String filepath) throws Exception {
        long startTime = System.currentTimeMillis();
        String url = StrUtil.format("{}/api/consumer/res/download/{}?t={}", getServerHost(), filepath, startTime);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "gmail-cloud")
                .addHeader("Cookie", getServerCookie())
                .get()
                .build();
        try (Response response = getUploadClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("请求主控下载接口失败,url: " + url + ",状态码:" + response.code());
            }
            log.info("请求主控下载接口:【{}】,cost:【{}】ms", url, System.currentTimeMillis() - startTime);
            return saveResponseToFile(groupTaskId, parentPath, url, response);
        }
    }

    protected File saveResponseToFile(String groupTaskId, Path parentPath, String url, Response response) throws IOException {
        String responseBody = response.body().string();
        String downloadFilename = response.header("Content-Disposition");
        if (StrUtil.isBlank(downloadFilename)) {
            downloadFilename = URLDecoder.decode(url.substring(url.lastIndexOf("/") + 1), StandardCharsets.UTF_8);
        }
        downloadFilename = downloadFilename.replaceAll(".*filename=\"?([^\"]+)\"?.*", "$1");
        downloadFilename = URLDecoder.decode(downloadFilename, StandardCharsets.UTF_8);
        log.info("请求主控下载接口:【{}】,结果文件名:【{}】", url, downloadFilename);
        File file = getResultFile(parentPath, groupTaskId, downloadFilename);
        if (file.exists()) {
            file.delete();
        }
        if (responseBody.startsWith("\uFEFF")) {// 去掉BOM头
            responseBody = responseBody.substring(1);
        }
        FileUtil.writeUtf8String(responseBody, file);
        return file;
    }

    public File getResultFile(Path parentPath, String groupTaskId, String filename) {
        return SieveActiveTaskUtils.getResultDir(parentPath, groupTaskId).resolve(filename).toFile();
    }

    private Request getRequest(String url, String body) {
        Headers headers = new Headers.Builder()
                .add("User-Agent", "gmail-cloud")
                .add("Cookie", getServerCookie())
                .build();
        RequestBody requestBody = RequestBody.create(body, MediaType.get("application/json; charset=utf-8"));
        return new Request.Builder()
                .url(url)
                .headers(headers)
                .post(requestBody)
                .build();
    }

    private String getServerHost() {
        return paramsService.getCloudMasterURL();
    }

    private String getServerCookie() {
        return paramsService.getCloudMasterCookie();
    }

    private OkHttpClient getDefaultClient() {
        return defaultClient.newBuilder()
//                .addNetworkInterceptor(bodyLoggingInterceptor)
                .build();
    }

    private OkHttpClient getUploadClient() {
        return defaultClient.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .callTimeout(120, TimeUnit.SECONDS)
//                .addNetworkInterceptor(basicLoggingInterceptor)
                .build();
    }
}
