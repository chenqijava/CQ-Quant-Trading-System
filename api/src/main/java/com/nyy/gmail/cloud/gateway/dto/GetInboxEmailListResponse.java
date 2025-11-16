package com.nyy.gmail.cloud.gateway.dto;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class GetInboxEmailListResponse extends GatewayResultBase {

    private Integer has_more;

    @Data
    public static class Thread {
        private ThreadInner thread;
        public Thread () {}
        public Thread(ThreadInner thread) {
            this.thread = thread;
        }
    }

    @Data
    public static class ThreadInner {
        private String subject;
        private String summary;
        private String timestamp;
        private String thread_id;
        private List<Message> message_list;
        private List<Message> messages;

        public List<Message> getMessage_list() {
            if (messages != null) {
                message_list = messages;
                messages = null;
            }
            return message_list;
        }
    }

    @Data
    public static class Message {
        private String message_id;
        private Profile sender;
        private String timestamp;
        private String summary;
        private List<String> labels;
    }

    private List<Thread> thread_list;

    private List<ThreadInner> inbox;

    public List<Thread> getThread_list() {
        if (inbox != null) {
            thread_list = inbox.stream().map(GetInboxEmailListResponse.Thread::new).collect(Collectors.toList());
            inbox = null;
        }
        return thread_list;
    }

    private Integer gm_seq;
}
