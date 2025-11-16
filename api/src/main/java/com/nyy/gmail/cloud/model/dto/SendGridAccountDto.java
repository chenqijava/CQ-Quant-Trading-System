package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class SendGridAccountDto {


    private String email;

    private String apiKey;

    private String userID;
}
