package com.nyy.gmail.cloud.utils;

import java.util.UUID;

public class TokenUtils {
    /**
     * 生成随机token
     * @return
     */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
