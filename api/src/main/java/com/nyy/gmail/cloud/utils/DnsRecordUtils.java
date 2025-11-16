package com.nyy.gmail.cloud.utils;

import com.alibaba.fastjson2.JSON;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DnsRecordUtils {

    private static final String API_KEY = "xAIMsjGAnqQFGHh_EW-B9N2yvOcNTlW8Q_TWn6Rb";

    /**
     *
     * @return 返回zoneId
     */
    public static String getZoneId(){
        // 构建请求URL
        String url = "https://api.cloudflare.com/client/v4/zones/";
        // 创建请求头
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get();
        // 添加认证头
        builder.addHeader("Authorization", "Bearer " + API_KEY);
        Request request = builder.build();

        try {
            // 执行请求
            OkHttpClient client = OkHttpClientFactory.getDefaultClient();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                // 解析JSON响应，获取第一个zone的id
                com.alibaba.fastjson2.JSONObject jsonObject = JSON.parseObject(responseBody);
                String zoneId = jsonObject.getJSONArray("result").getJSONObject(0).getString("id");
                log.info("获取ZoneId成功: {}", zoneId);
                return zoneId;
            } else {
                String errorResponse = response.body().string();
                log.error("DNS记录导出失败: {}", errorResponse);
                throw new CommonException("DNS记录导出失败: " + errorResponse);
            }
        } catch (IOException e) {
            log.error("DNS记录导出请求异常: ", e);
            throw new CommonException("DNS记录导出请求异常: " + e.getMessage());
        }
    }


    /**
     * 解析DNS记录列表结果
     * @param dnsRecordResult DNS记录查询结果
     * @return 包含记录列表和总数的信息
     */
    public static List<Map<String, String>> getDnsRecord(String dnsRecordResult){
        List<Map<String, String>> records = new ArrayList<>();

        try {
            com.alibaba.fastjson2.JSONObject jsonObject = JSON.parseObject(dnsRecordResult);
            com.alibaba.fastjson2.JSONArray resultArray = jsonObject.getJSONArray("result");

            // 遍历所有记录
            for (int i = 0; i < resultArray.size(); i++) {
                com.alibaba.fastjson2.JSONObject record = resultArray.getJSONObject(i);

                // 提取需要的字段
                Map<String, String> recordInfo = new HashMap<>();
                recordInfo.put("id", record.getString("id"));
                recordInfo.put("name", record.getString("name"));
                recordInfo.put("content", record.getString("content"));
                records.add(recordInfo);
            }
        } catch (Exception e) {
            log.error("解析DNS记录数据失败", e);
            throw new CommonException("解析DNS记录数据失败: " + e.getMessage());
        }
        return records;
    }

    /**
     * 批量插入DNS记录
     *
     * @param zoneId - 区域ID
     * @param records - DNS记录信息
     * @return 创建结果
     */
    public static String createDnsRecords(String zoneId, List<Map<String,Object>> records){
        // 构建请求URL
        String url = "https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records/batch";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("posts",records);
        String jsonBody = JSON.toJSONString(requestBody);

        // 创建请求
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        // 创建请求头
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);

        // 添加认证头
        builder.addHeader("Authorization", "Bearer " + API_KEY);
        Request request = builder.build();

        try {
            // 执行请求
            OkHttpClient client = OkHttpClientFactory.getDefaultClient();
            Response response = client.newCall(request).execute();

            // 确保响应体能够被正确关闭
            try (ResponseBody responseBody = response.body()) {
                if (response.isSuccessful() && responseBody != null) {
                    String responseString = responseBody.string();
                    return responseString;
                } else if (responseBody != null) {
                    String errorResponse = responseBody.string();
                    throw new CommonException("DNS记录删除失败: " + errorResponse);
                } else {
                    throw new CommonException("DNS记录删除失败: 响应体为空");
                }
            }
        } catch (IOException e) {
            throw new CommonException("DNS记录删除请求异常: " + e.getMessage());
        }
    }

    /**
     * 批量删除DNS记录
     *
     * @param zoneId - 区域ID
     * @param dnsRecordIds - DNS记录ids
     * @return 删除结果
     */
    public static String deleteDnsRecords(String zoneId, List<String> dnsRecordIds){
        // 构建请求URL
        String url = "https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records/batch";
        List<Map<String, String>> deletes = new ArrayList<>();
        for (String id : dnsRecordIds) {
            Map<String, String> deleteItem = new HashMap<>();
            deleteItem.put("id", id);
            deletes.add(deleteItem);
        }
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deletes", deletes);
        String jsonBody = JSON.toJSONString(requestBody);

        // 创建请求
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        // 创建请求头
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);

        // 添加认证头
        builder.addHeader("Authorization", "Bearer " + API_KEY);
        Request request = builder.build();

        try {
            // 执行请求
            OkHttpClient client = OkHttpClientFactory.getDefaultClient();
            Response response = client.newCall(request).execute();

            // 确保响应体能够被正确关闭
            try (ResponseBody responseBody = response.body()) {
                if (response.isSuccessful() && responseBody != null) {
                    String responseString = responseBody.string();
                    log.info("DNS记录删除成功: {}", responseString);
                    return responseString;
                } else if (responseBody != null) {
                    String errorResponse = responseBody.string();
                    log.error("DNS记录删除失败: {}", errorResponse);
                    throw new CommonException("DNS记录删除失败: " + errorResponse);
                } else {
                    log.error("DNS记录删除失败: 响应体为空");
                    throw new CommonException("DNS记录删除失败: 响应体为空");
                }
            }
        } catch (IOException e) {
            log.error("DNS记录删除请求异常: ", e);
            throw new CommonException("DNS记录删除请求异常: " + e.getMessage());
        }
    }


    /**
     * 导出DNS记录
     *
     * @param zoneId   区域ID
     * @return 导出结果
     */
    public static String listDnsRecord(String zoneId, Integer page ,Integer per_page){
        // 构建请求URL
        String url = "https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records/?type=CNAME&page="+page+"&per_page="+per_page;

        // 创建请求头
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get();

        // 添加认证头
        builder.addHeader("Authorization", "Bearer " + API_KEY);
        Request request = builder.build();

        try {
            // 执行请求
            OkHttpClient client = OkHttpClientFactory.getDefaultClient();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                return responseBody;
            } else {
                String errorResponse = response.body().string();
                log.error("DNS记录导出失败: {}", errorResponse);
                throw new CommonException("DNS记录导出失败: " + errorResponse);
            }
        } catch (IOException e) {
            log.error("DNS记录导出请求异常: ", e);
            throw new CommonException("DNS记录导出请求异常: " + e.getMessage());
        }
    }
}
