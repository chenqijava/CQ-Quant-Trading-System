package com.nyy.gmail.cloud.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.model.bo.UploadFileBO;
import com.nyy.gmail.cloud.model.dto.SendMsgFileDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * tg bot api - 机器人api
 *
 * @Author: wjx
 * @Date: 2023/6/15 15:01
 */
@Slf4j
public class TgBotApi {

    /**
     * 解绑
     */
    public static final String TGBOT_UNBIND_URL = "/api/common/auth/unbind";

    /**
     * 发送消息
     */
    public static final String TGBOT_SEND_MESSAGE_URL = "/api/common/auth/sendMsg";
    /**
     * 发送文件
     */

    public static final String TGBOT_SEND_DOCUMENT_URL = "/api/common/auth/sendDocument";
    /**
     * 发送多文件
     */
    public static final String TGBOT_SEND_DOCUMENTS_URL = "/api/common/auth/sendDocuments";

    public static final String TGBOT_UPLOAD_FILE_URL = "/api/consumer/res/upload/crm";

    /**
     * 上传文件,默认上传
     *
     * @param domain
     * @param token
     * @param file   文件地址
     * @return
     * @throws Exception
     */
    public static UploadFileBO uploadFile(String domain, String token, File file) throws Exception {
        String url = String.format(domain + TGBOT_UPLOAD_FILE_URL);
        HttpRequest request = HttpRequest.post(url)
                .header("TOKEN", token)
                .form("file", file);
        String body = file.getName();
        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            throw new Exception("接口请求失败,状态码:" + response.getStatus());
        }
        String responseBody = response.body();
        log.info("请求TG Bot接口:【{}】,参数:【{}】,结果:【{}】", url, body, responseBody);
        JSONObject jsonObject = JSONObject.parseObject(responseBody);
        if (jsonObject.getInteger("code") != 1) {
            throw new Exception("上传失败,错误原因:" + jsonObject.getString("message"));
        }
        return jsonObject.getObject("data", UploadFileBO.class);
    }


    public static String notifyContent(String filename, String content) {
        return formatContentFile("", filename, content);
    }

    public static String formatContentFile(String title, String filename, String content) {
        if (StringUtils.isBlank(title)) {
            title = "筛活通知";
        }
        return StrUtil.format("*{}*\n\n\n*文件:*  {}\n\n*结果:*  {}",
                applyMarkdownV2Formatting(title),
                applyMarkdownV2Formatting(filename),
                applyMarkdownV2Formatting(content));
    }

    public static String formatContent(String content, String orderId) {
        return formatContent("", content, orderId);
    }


    public static String formatContent(String title, String content, String orderId) {
        if (StringUtils.isBlank(title)) {
            title = "筛活通知";
        }
        if (StrUtil.isEmpty(orderId)) {
            return StrUtil.format("*{}*\n\n*内容:*  {} ",
                    applyMarkdownV2Formatting(title),
                    applyMarkdownV2Formatting(content));
        }
        return StrUtil.format("*{}*\n\n*内容:*  {} \n\n*订单号ID:* `{}`",
                applyMarkdownV2Formatting(title),
                applyMarkdownV2Formatting(content),
                applyMarkdownV2Formatting(orderId));
    }

//    public static String formatContent(String title, String content, String taskName, String taskLink, String orderId) {
//        if (StringUtils.isBlank(title)) {
//            title = "筛活通知";
//        }
//        return StrUtil.format("*{}*\n\n\n*通知内容:* {}\n\n*云控任务:* [{}]({}) \n\n*订单号ID:* `{}`",
//                applyMarkdownV2Formatting(title),
//                applyMarkdownV2Formatting(content),
//                applyMarkdownV2Formatting(taskName),
//                taskLink,
//                applyMarkdownV2Formatting(orderId));
//    }

    public static String formatContent(String title, String content, String orderId, List<Map<String, String>> tasks) {
        if (StringUtils.isBlank(title)) {
            title = "筛活通知";
        }

        String taskCotnent = tasks.stream().map(task -> {
                    return StrUtil.format("[{}]({})", applyMarkdownV2Formatting(task.get("taskName")), task.get("taskLink"));
                }
        ).collect(Collectors.joining("\n"));
        return StrUtil.format("*{}*\n\n\n*通知内容:* {}\n\n*云控任务:* {}\n\n*订单号ID*: `{}`",
                applyMarkdownV2Formatting(title),
                applyMarkdownV2Formatting(content),
                taskCotnent,
                applyMarkdownV2Formatting(orderId));
    }

    public static String formatContent(String title, String content, String taskName, String taskLink, String orderId) {
        if (StringUtils.isBlank(title)) {
            title = "筛活通知";
        }
        return StrUtil.format("*{}*\n\n\n*通知内容:* {}\n\n*云控任务:* [{}]({})\n\n订单号ID: `{}`",
                applyMarkdownV2Formatting(title),
                applyMarkdownV2Formatting(content),
                applyMarkdownV2Formatting(taskName),
                taskLink,
                applyMarkdownV2Formatting(orderId));
    }

    public static String applyMarkdownV2Formatting(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        // 过滤特殊字符
        String filteredText = text.replaceAll("[_*\\[\\]()~>`#+\\-=|{}.!]", "\\\\$0");
        return filteredText;
    }

    /**
     * 发送消息
     *
     * @param domain
     * @param token
     * @param accID
     * @param content
     * @throws Exception
     */
    public static void sendMessage(String domain, String token, String accID, String content) throws Exception {
        String url = String.format(domain + TGBOT_SEND_MESSAGE_URL);
        JSONObject json = new JSONObject();
        json.put("token", token);
        json.put("accID", accID);
        json.put("content", content);
        json.put("parseMode", "MarkdownV2");//markdown格式
        requestApi(url, json);
    }

    /**
     * 发送文件
     *
     * @param domain
     * @param token
     * @param accID
     * @param caption
     * @param filepath
     * @throws Exception
     */
    public static void sendDocument(String domain, String token, String accID, String caption, String filename, String filepath) throws Exception {
        String url = String.format(domain + TGBOT_SEND_DOCUMENT_URL);

        JSONObject json = new JSONObject();
        json.put("token", token);
        json.put("accID", accID);
        json.put("caption", caption);
        json.put("filepath", filepath);
        json.put("filename", filename);

        requestApi(url, json);

    }

    public static void sendDocuments(String domain, String token, String accID, String content, List<SendMsgFileDTO> files) throws Exception {
        String url = String.format(domain + TGBOT_SEND_DOCUMENTS_URL);
        JSONObject json = new JSONObject();
        json.put("token", token);
        json.put("accID", accID);
        json.put("content", content);
        json.put("files", files);

        requestApi(url, json);

    }


    /**
     * 发送文件文件名超长,进行截断(最长64个字符)
     *
     * @param filename
     * @return
     */
    public static String cutFileName(String filename) {

        int length = 64;
        if (StrUtil.isBlank(filename)) {
            return "";
        }
        if (filename.length() <= 64) {
            return filename;
        }
        String[] names = filename.split("-");
        //name = 越南-活跃7天-500-未成功号-1182.txt
        // 从names中获取到未成功号-1182.txt
        String suffix = String.join("-", Arrays.copyOfRange(names, names.length - 2, names.length));
        String prefix = String.join("-", Arrays.copyOfRange(names, 0, names.length - 2));
        return prefix.substring(0, length - suffix.length() - 1) + "-" + suffix;
    }


    public static JSONObject unbind(String domain, String token, String accID, String platformUserID) throws Exception {
        String url = String.format(domain + TGBOT_UNBIND_URL);
        JSONObject json = new JSONObject();
        json.put("token", token);
        json.put("accID", accID);
        json.put("platform", "CRM");
        json.put("platformUserID", platformUserID);
        return requestApi(url, json);

    }


    private static JSONObject requestApi(String url, JSONObject json) throws Exception {
        String body = json.toJSONString();
        try {
            HttpRequest request = HttpRequest.post(url).body(body);
            HttpResponse response = request.execute();
            if (response.getStatus() != 200 && response.getStatus() != 504) {
                throw new Exception("接口请求失败,状态码:" + response.getStatus());
            }
            if (response.getStatus() == 504) {
                JSONObject jsonObject = JSONObject.parseObject("{\"code\":1}");
                return jsonObject;
            }
            String responseBody = response.body();
            log.info("请求TG Bot接口:【{}】,参数:【{}】,结果:【{}】", url, body, responseBody);
            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            if (jsonObject.getInteger("code") != 1) {
                throw new Exception("发送失败,错误原因:" + jsonObject.getString("message"));
            }
            return jsonObject;
        }catch (Exception e){
            log.error("请求TG Bot接口失败,接口:{},参数:{},错误信息:{}",url,body,e.getMessage());
            throw e;
        }
    }


}
