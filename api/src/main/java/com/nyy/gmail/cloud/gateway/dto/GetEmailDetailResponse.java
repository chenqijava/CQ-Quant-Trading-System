package com.nyy.gmail.cloud.gateway.dto;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class GetEmailDetailResponse extends GatewayResultBase {

    private List<Thread> threads;

    @Data
    public static class Thread {
        private String thread_id;
        private List<Message> messages;
    }

    @Data
    public static class Message {
        private String message_id;
        private MessageContent message_content;
    }

    @Data
    public static class MessageContent {
        private List<Profile> receivers;
        private String subject;
        private ContentDetail content_details;
        private String summary;
        private Profile sender;
        private List<Attachment> attachments;
        private String timestamp;
    }

    @Data
    public static class Attachment {
        private AttachmentInner info;
    }

    @Data
    public static class AttachmentInner {
        private AttachmentDetail details;
    }

    @Data
    public static class AttachmentDetail {
        private String url;
        private String filename;
        private String mime_type;
        private Integer size;
    }

    @Data
    public static class ContentDetail {
        private List<ContentsInner> contents;
        private ContentBody style;
        private String root;
    }

    @Data
    public static class ContentsInner {
        private ContentBody body;
        public ContentsInner () {}
        public ContentsInner (ContentBody body) {
            this.body = body;
        }
    }

    @Data
    public static class ContentBody {
        private String text;
        public ContentBody(String text) {
            this.text = text;
        }
        public ContentBody() {}
    }

    public static GetEmailDetailResponse buildFromJson(String response) {
        if (StringUtils.isEmpty(response)) {
            GetEmailDetailResponse getEmailDetailResponse = new GetEmailDetailResponse();
            getEmailDetailResponse.setCode(-1);
            getEmailDetailResponse.setMsg("请求返回值为空");
            return getEmailDetailResponse;
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(response);
            GetEmailDetailResponse detailResponse = new GetEmailDetailResponse();
            detailResponse.setCode(jsonObject.getInteger("code"));
            detailResponse.setMsg(jsonObject.getString("msg"));
            JSONObject threadMap = jsonObject.getJSONObject("thread_map");
            if (threadMap == null) {
                return detailResponse;
            }
            List<Thread> threads = new ArrayList<>();
            for (String key : threadMap.keySet()) {
                Thread thread = new Thread();
                thread.setThread_id(key);

                List<Message> messages = new ArrayList<>();
                JSONArray jsonArray = threadMap.getJSONArray(key);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject msg = jsonArray.getJSONObject(i);
                    Message message = new Message();
                    message.setMessage_id(msg.getString("message_id"));

                    MessageContent messageContent = new MessageContent();
                    messageContent.setSubject(msg.getString("subject"));
                    messageContent.setTimestamp(msg.getString("timestamp"));
                    messageContent.setSummary(msg.getString("summary"));
                    messageContent.setSender(msg.getObject("sender", Profile.class));
                    messageContent.setReceivers(msg.getJSONArray("receivers").toJavaList(Profile.class));
                    ContentDetail contentDetail = new ContentDetail();
                    contentDetail.setContents(List.of(new ContentsInner(new ContentBody(msg.getString("content")))));
                    messageContent.setContent_details(contentDetail);
                    message.setMessage_content(messageContent);

                    messages.add(message);
                }
                thread.setMessages(messages);
                threads.add(thread);
            }
            detailResponse.setThreads(threads);

            return detailResponse;
        } catch (Exception e) {
            GetEmailDetailResponse getEmailDetailResponse = new GetEmailDetailResponse();
            getEmailDetailResponse.setCode(-1);
            getEmailDetailResponse.setMsg("请求返回值解析错误");
            return getEmailDetailResponse;
        }
    }
}
