package com.nyy.gmail.cloud.gateway.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class YahooEmailDetailResponse {
    private YahooEmailMessage message;
    private YahooEmailBody simpleBody;

    @Data
    public static class YahooEmailMessage {
        private YahooEmailListMessageHeader headers;
        private String id;
        private String snippet;
        private String conversationId;
        private Map<String, Boolean> flags;
    }

    @Data
    public static class YahooEmailBody {
        private String html;
    }

    @Data
    public static class YahooEmailListMessageHeader {
        private String subject;
        private List<Profile> from;
        private List<Profile> to;
        private List<Profile> replyTo;
        private Long date;
    }
}
