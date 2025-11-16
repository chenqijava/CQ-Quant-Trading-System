package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

@Data
public class WorkspaceAccountDto {
    private String jsonFilePath;

    private String emailFilePath;

    private String type;

    private String email;

    private String clientId;

    private String clientSecret;

    private String refreshToken;

    private String groupId;

    private String userID;
}
