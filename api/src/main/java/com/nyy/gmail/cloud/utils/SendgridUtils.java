package com.nyy.gmail.cloud.utils;

import com.alibaba.fastjson2.JSON;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class SendgridUtils {

    public static String sendEmail(Socks5 socks5, String fromEmail, String apiKey, List<String> to, String subject, String contentHtml) throws Exception {
        Date start = new Date();
        log.info(apiKey + " 开始发送邮件," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            String uuid = UUIDUtils.get32UUId();
            List<Map<String, Object>> personalizations = new ArrayList();
            personalizations.add(Map.of("to", to.stream().map(e -> Map.of("email", e)).toList(), "subject", subject, "custom_args", Map.of("uuid", uuid)));

            OkHttpClient client = OkHttpClientFactory.getSendGrid(socks5);
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("personalizations", personalizations,"content", List.of(Map.of(
                    "type", "text/html",
                    "value", contentHtml)
            ),"from", Map.of("email", fromEmail), "reply_to", Map.of("email", fromEmail)));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://api.sendgrid.com/v3/mail/send");
            request = builder.post(body).addHeader("Authorization", "Bearer " + apiKey).build();

            log.info(apiKey + " 发送邮件 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(apiKey + " 发送邮件,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                    log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info(apiKey + " " + "result: " + respStr.replace("\n", ""));
                    if (respStr.contains("Maximum credits exceeded")) {
                        throw new CommonException(ResultCode.INSUFFICIENT_USER_BALANCE);
                    }
                    if (response.code() == 202) {
                        return uuid;
                    }
                }
            }
            throw new Exception("发送邮件失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(apiKey + " 发送邮件,失败: {}", e.getMessage());
            log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }
}
