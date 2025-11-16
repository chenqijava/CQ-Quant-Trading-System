package com.nyy.gmail.cloud.utils;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SpamAssassinClient {

    private final String host;
    private final int port;
    private final int timeoutMs;

    public SpamAssassinClient(String host, int port, int timeoutMs) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    /** PING 测试 */
    public boolean ping() throws IOException {
        String response = sendCommand("PING SPAMC/1.2\r\n\r\n", null);
        return response.contains("PONG");
    }

    /** CHECK 检测垃圾邮件 */
    public String check(String emailContent) throws IOException {
        return sendCommand("CHECK SPAMC/1.2\r\n", prepareEmail(emailContent));
    }

    /** REPORT 获取详细规则 */
    public String report(String emailContent) throws IOException {
        return sendCommand("REPORT SPAMC/1.2\r\n", prepareEmail(emailContent));
    }

    /** 核心通信方法 */
    private String sendCommand(String command, byte[] emailBytes) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(timeoutMs);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            if (emailBytes != null) {
                String header = command +
                        "Content-length: " + emailBytes.length + "\r\n" +
                        "\r\n";
                out.write(header.getBytes(StandardCharsets.ISO_8859_1));
                out.write(emailBytes);
            } else {
                // 仅发送命令（如 PING）
                out.write(command.getBytes(StandardCharsets.ISO_8859_1));
            }

            out.flush();

            // 读取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\r\n");
            }

            return response.toString();
        }
    }

    /** 准备邮件，保证换行和空行正确 */
    private byte[] prepareEmail(String emailContent) {
        // 替换所有 \n 为 \r\n
        String normalized = emailContent.replaceAll("(?<!\r)\n", "\r\n");

        // 保证头部和正文之间有空行
        if (!normalized.contains("\r\n\r\n")) {
            normalized = normalized + "\r\n\r\n";
        }

        return normalized.getBytes(StandardCharsets.ISO_8859_1);
    }

    public static void main(String[] args) throws Exception {
        SpamAssassinClient client = new SpamAssassinClient("192.168.10.231", 783, 30000);

        // 1. PING 测试
        System.out.println("PING: " + client.ping());

        // 2. 测试邮件
        String email = "From: 1311949161@qq.com\n" +
                "To: c1311949161@gmail.com\n" +
                "Subject: Cheap Viagra\n\n" +
                "Buy cheap Viagra now! Click here: http://spam.example.com";

        // 3. CHECK
        String checkResult = client.check(email);
        System.out.println("CHECK result:\n" + checkResult);

        // 4. REPORT
        String reportResult = client.report(email);
        System.out.println("REPORT result:\n" + reportResult);
    }
}