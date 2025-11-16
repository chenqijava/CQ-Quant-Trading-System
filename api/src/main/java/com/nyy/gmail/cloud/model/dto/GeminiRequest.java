package com.nyy.gmail.cloud.model.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeminiRequest {
    public String model;
    public List<Prompt> prompt;
    public Integer max_output_tokens;

    static class Prompt {
        public String role;
        public List<Content> parts;

        static class Content {
            public String text;

            public Content(String text) {
                this.text = text;
            }
        }

        public Prompt(String author, String text) {
            if (author.equals("system")) {
                this.role = "model";
            } else {
                this.role = author;
            }
            this.parts = Collections.singletonList(new Content(text));
        }
    }

    public static GeminiRequest convert(OpenAIRequest openAIRequest) {
        GeminiRequest gemini = new GeminiRequest();
        gemini.model = openAIRequest.model;
        gemini.max_output_tokens = openAIRequest.max_tokens;

        List<GeminiRequest.Prompt> prompts = new ArrayList<>();
        for (OpenAIRequest.Message msg : openAIRequest.messages) {
            prompts.add(new GeminiRequest.Prompt(msg.role, msg.content));
        }
        gemini.prompt = prompts;

        return gemini;
    }
}
