package com.nyy.gmail.cloud.utils;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.List;

@Slf4j
@Component
public class GoogleDocUtils {
    public static final String BASE_DOMAIN = "google doc";
    @Getter
    private static String baseUrl;
    private static String clientId;
    private static String clientSecret;
    private static String refreshToken;
    private static String testTokenDocId;

    @Value("${googleDoc.baseUrl}")
    public void setBaseUrl(String baseUrl) {
        GoogleDocUtils.baseUrl = baseUrl;
    }

    @Value("${googleDoc.clientId}")
    public void setClientId(String clientId) {
        GoogleDocUtils.clientId = clientId;
    }

    @Value("${googleDoc.clientSecret}")
    public void setClientSecret(String clientSecret) {
        GoogleDocUtils.clientSecret = clientSecret;
    }

    @Value("${googleDoc.refreshToken}")
    public void setRefreshToken(String refreshToken) {
        GoogleDocUtils.refreshToken = refreshToken;
    }

    @Value("${googleDoc.testTokenDocId}")
    public void setTestTokenDocId(String testTokenDocId) {
        GoogleDocUtils.testTokenDocId = testTokenDocId;
    }

    public static String addDoc(String id, String url)
            throws IOException {
        String text = url;
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build());
        Docs docService = new Docs.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), requestInitializer)
                .setApplicationName("GOOGLE DOC")
                .build();
        Drive driverService = new Drive.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), requestInitializer)
                .setApplicationName("GOOGLE DRIVE")
                .build();
        Document document = new Document().setTitle(id);
        Document createdDoc = docService.documents().create(document).execute();
        String docId = createdDoc.getDocumentId();
        InsertTextRequest insertTextRequest = new InsertTextRequest()
                .setLocation(new Location().setIndex(1)) // 文档起始位置（index=1 是文档开头）
                .setText(text);

        // 步骤2：为插入的文本设置链接（关联 URL）
        UpdateTextStyleRequest updateTextStyleRequest = new UpdateTextStyleRequest()
                .setRange(new Range()
                        .setStartIndex(1) // 文本起始位置（与插入位置一致）
                        .setEndIndex(1 + text.length())) // 文本结束位置（起始+文本长度）
                .setTextStyle(new TextStyle()
                        .setLink(new Link().setUrl(url))) // 设置链接 URL
                .setFields("link"); // 仅更新 link 字段（优化性能）

        // 批量执行两个请求（插入文本 + 设置链接）
        List<com.google.api.services.docs.v1.model.Request> requests = Arrays.asList(
                new com.google.api.services.docs.v1.model.Request().setInsertText(insertTextRequest),
                new Request().setUpdateTextStyle(updateTextStyleRequest)
        );

        BatchUpdateDocumentRequest batchUpdateRequest = new BatchUpdateDocumentRequest()
                .setRequests(requests);

        docService.documents().batchUpdate(docId, batchUpdateRequest).execute();

        Permission permission = new Permission()
                .setType("anyone") // 共享对象：所有人
                .setRole("reader") // 权限：仅查看（可选：writer=可编辑，commenter=可评论）
                .setAllowFileDiscovery(false); // 是否允许通过搜索发现（false=不允许）

        // 执行共享操作
        driverService.permissions().create(docId, permission)
                .setSendNotificationEmail(false) // 是否发送通知邮件（false=不发送）
                .execute();
        return baseUrl + docId;
    }

    @Async("async")
    @Scheduled(cron = "0 30 * * * *")
    public void run() {
        try {
            log.info("开始RefreshTokenJob");
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build());
            Docs docService = new Docs.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), requestInitializer)
                    .setApplicationName("GOOGLE DOC")
                    .build();
            Document doc = docService.documents().get(testTokenDocId).execute();
        } catch (IOException e) {
            log.error("Refresh Google Doc Token ERROR:" + e.getMessage(), e);
        } finally {
            log.info("结束RefreshTokenJob");
        }
    }
}
