package com.nyy.gmail.cloud.gateway.dto;

import lombok.Data;

@Data
public class AccountAuthResponse extends GatewayResultBase {

    @lombok.Data
    public static class Data {
        private String issueAdvice;
        private String Expiry;
        private String ExpiresInDurationSec;
        private String storeConsentRemotely;
        private String isTokenSnowballed;
        private String grantedScopes;
        private String itMetadata;
        private String it;
        private String TokenEncrypted;
    }

    private Data data;
    private String deviceinfo;
}
