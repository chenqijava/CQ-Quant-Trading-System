package com.nyy.gmail.cloud.gateway.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@ToString
@Data
public class OutlookGraphEmail {

    private String id;

    private String createdDateTime;

    private String lastModifiedDateTime;

    private String changeKey;

    private List<String> categories;

    private String receivedDateTime;

    private String sentDateTime;

    private boolean hasAttachments;

    private String internetMessageId;

    private String subject;

    private String bodyPreview;

    private String importance;

    private String parentFolderId;

    private String conversationId;

    private String conversationIndex;

    private boolean isDeliveryReceiptRequested;

    private boolean isReadReceiptRequested;

    private boolean isRead;

    private boolean isDraft;

    private String webLink;

    private String inferenceClassification;

    private Body body;

    private EmailAddress sender;

    private EmailAddress from;

    private List<EmailAddress> toRecipients;

    private List<EmailAddress> ccRecipients;

    private List<EmailAddress> bccRecipients;

    private List<EmailAddress> replyTo;

    private Flag flag;

    @Data
    public static class Body {
        private String contentType;

        private String content;
    }

    @Data
    public static class EmailAddress {
        private Email emailAddress;
    }

    @Data
    public static class Email {
        private String name;
        private String address;
    }

    @Data
    public static class Flag {
        private String flagStatus;
    }
}
