package com.nyy.gmail.cloud.model.dto;

import java.util.List;

public class OpenAIRequest {
    public String model;
    public List<Message> messages;
    public Integer max_tokens;
    public Boolean stream;

    static class Message {
        public String role; // system / user / assistant
        public String content;
    }
}
