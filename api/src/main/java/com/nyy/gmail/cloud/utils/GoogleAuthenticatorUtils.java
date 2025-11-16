package com.nyy.gmail.cloud.utils;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

import java.util.HashMap;
import java.util.Map;

public class GoogleAuthenticatorUtils {

    public static boolean verifyCode (String code, String secretKey) {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        return gAuth.authorize(secretKey, Integer.valueOf(code));
    }

    public static String email = "TNTMailBox";  // 可换为用户名或邮箱
    public static String issuer = "TNTMailBox";          // 显示在 Google Authenticator 中的服务名

    public static Map<String, String> getGoogleAuthUrl() {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();

        String secret = key.getKey();

        String qrUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, email, key);
        Map<String, String> map = new HashMap<>();
        map.put("secret", secret);
        map.put("url", qrUrl);
        return map;
    }

    public static String convertAuthUrl(String email, String secret) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, email, secret, issuer);
    }

    public static int getToken(String secret) {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder().build();
        GoogleAuthenticator gAuth = new GoogleAuthenticator(config);

        // 获取当前时间戳对应的验证码（6 位）
        int code = gAuth.getTotpPassword(secret);
        return code;
    }
}
