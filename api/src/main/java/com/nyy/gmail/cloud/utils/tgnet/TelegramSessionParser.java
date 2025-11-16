package com.nyy.gmail.cloud.utils.tgnet;

import java.io.*;
import java.nio.file.*;
import java.util.Base64;

public class TelegramSessionParser {

    /**
     * Base64解码，处理URL安全的Base64编码
     */
    public static byte[] base64Decode(String string) throws Exception {
        string = string.replace("-", "+").replace("_", "/");
        for (int n = 0; n < 3; n++) {
            try {
                String data = string + "=".repeat(n);
                return Base64.getDecoder().decode(data);
            } catch (Exception e) {
                // 继续尝试
            }
        }
        throw new Exception("invalid base64 data");
    }

    /**
     * 将文件内容转为base64
     */
    public static String fileToBase64(String filePath) {
        try {
            byte[] content = Files.readAllBytes(Paths.get(filePath));
            return Base64.getEncoder().encodeToString(content);
        } catch (IOException e) {
            System.out.println("读取文件 " + filePath + " 时出错: " + e.getMessage());
            return null;
        }
    }
}
