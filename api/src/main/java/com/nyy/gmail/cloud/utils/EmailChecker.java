package com.nyy.gmail.cloud.utils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.service.Socks5Service;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Slf4j
public class EmailChecker {
    private final int timeout;
    private final boolean verbose;
    private final RedisUtil redisUtil;
    private final Socks5Service  socks5Service;

    private final List<String> userAgents = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"
    );

    public EmailChecker(int timeout, boolean verbose,  RedisUtil redisUtil, Socks5Service socks5Service) {
        this.timeout = timeout;
        this.verbose = verbose;
        this.redisUtil = redisUtil;
        this.socks5Service = socks5Service;
    }

    /** 验证邮箱格式 */
    public boolean validateEmailFormat(String email) {
        String regex = "^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$";
        return Pattern.matches(regex, email);
    }

    /** 是否是 QQ 邮箱 */
    public boolean isQqEmail(String email) {
        String domain = email.split("@")[1].toLowerCase();
        return Arrays.asList("qq.com", "vip.qq.com", "foxmail.com").contains(domain);
    }

    /** QQ 邮箱规则校验 */
    public String validateQqEmailFormat(String email) {
        String username = email.split("@")[0];

        if (username.isEmpty()) {
            return "用户名不能为空";
        }
        if (username.matches("\\d+")) {
            if (username.length() < 5 || username.length() > 12) {
                return "QQ号长度异常";
            }
            return "QQ号格式正确";
        }
        if (username.matches("^[a-zA-Z][a-zA-Z0-9_.-]{2,17}$")) {
            return "英文邮箱格式正确";
        }
        return "不符合QQ邮箱用户名规则";
    }

    /** 获取 MX 记录 */
    public List<String> getMxRecords(String domain) {
        List<String> mxList = new ArrayList<>();
        try {
            Record[] records = new Lookup(domain, Type.MX).run();
            if (records != null) {
                for (Record r : records) {
                    MXRecord mx = (MXRecord) r;
                    mxList.add(mx.getTarget().toString());
                }
            }
        } catch (Exception e) {
            if (verbose) {
                System.out.println("❌ 获取MX记录失败: " + domain + " -> " + e.getMessage());
            }
        }
        return mxList;
    }

    /** 检查 SMTP 连接 */
    public Result checkSmtpConnection(String email) {
        if (!validateEmailFormat(email)) {
            return new Result(email, false, "邮箱格式无效");
        }

        String domain = email.split("@")[1];
        List<String> mxRecords = null;
        Object result = redisUtil != null ? redisUtil.get("MX_DOMAIN:" + domain) : null;
        if (result != null) {
            try {
                Gson gson = new Gson();
                java.lang.reflect.Type type = new TypeToken<List<String>>() {
                }.getType();
                mxRecords = gson.fromJson(result.toString(), type);
            } catch (Exception e) {}
        }
        if (mxRecords == null) {
            mxRecords = getMxRecords(domain);
            if (mxRecords != null && redisUtil != null) {
                redisUtil.set("MX_DOMAIN:" + domain, JSON.toJSONString(mxRecords), 60*60*24);
            }
        }

        if (mxRecords.isEmpty()) {
            return new Result(email, false, "无法获取MX记录");
        }

        for (String mx : mxRecords) {
            try (Socket socket = new Socket(mx, 25)) {
                socket.setSoTimeout(timeout * 1000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                reader.readLine(); // banner
                writer.write("HELO gmail.com\r\n");
                writer.flush();
                reader.readLine();

                writer.write("MAIL FROM:<c1312949262@gmail.com>\r\n");
                writer.flush();
                reader.readLine();

                writer.write("RCPT TO:<" + email + ">\r\n");
                writer.flush();
                String response = reader.readLine();

                writer.write("QUIT\r\n");
                writer.flush();

//                if (response != null) {
//                    log.info("RCPT: " + response + " email: " + email);
//                }

                List<String> others = List.of("550 blocked", "command rejected", "protocol error", "503 5.5.0 not available", "550 rejected",
                        "issue starttls", "sender not yet given", "issue a starttls",
                        "verification failed", "relay not permitted", "550 relay", "local policy violation",
                        "need mail before rcpt", "451 open relay not allowed", "554 smtp synchronization error", "450 4.8.1 try again later",
                        "host rejected", "550 (id:550:3:0)","503 bad sequence of commands", "452 4.5.3 resources restricted",
                        "550 reverse dns lookup failed", "please try", "message was rejected", "domain not found", "relay is not allowed",
                        "route requires encryption","410-failed","451 internal resource temporarily unavailable", "451-sender host",
                        "is receiving mail at a rate", "possibly forged hostname", "sender ip must resolve","need mail command", "bad reverse dns",
                        "too many errors", "mail command expected", "sender not specified","domain profile not found", "not yet authorized to deliver",
                        "temporary lookup failure", "send helo first", "do not relay", "try again", "network not allowed");

                List<String> noExists = List.of("user unknown", "no such user", "mailbox unavailable",
                        "recipient address rejected", "bad address email", "does not exist", "is inactive",
                        "not available email", "not be found", "invalid recipient", "and inactive", "no such recipient",
                        "user not found", "unknown user", "is no longer valid", "no mailbox",
                        "invalid address", "unrouteable address", "not a valid mailbox", "recipient rejected",
                        "mailbox is disabled", "out of storage space", "recipient unknown", "inbounds disabled",
                        "not configured to accept that recipient", "address rejected", "dosn't exist", "mailbox is full", "recipient is not exist",
                        "cannot deliver mail");

                List<String> spamhaus = List.of("spamhaus", "on our block list", "refused", "access denied", "/en/case?c=r0303&i=ip",
                        "envelope blocked", "ignore all incoming emails", "relaying denied", "blocked using", "is blacklisted",
                        "address blacklisted", "sender address rejected", "ip listed on", "valid mail from", "550 5.7.0 blocked",
                        "550 g_spam_allow_disable spf or rbl failure", "geoip greylisted", "detected as spam","listed in dns bl",
                        "rejected: blocked", "spf.pobox.com", "en/case?c=r0602&i=ip", "s=abusix&ip=", "spf soft");

                if (response != null) {
                    log.info("RCPT: " + response + " email: " + email);
                    if (!response.startsWith("250") && noExists.stream().noneMatch(e -> response.toLowerCase().contains(e)) && spamhaus.stream().noneMatch(e -> response.toLowerCase().contains(e)) && others.stream().noneMatch(e -> response.toLowerCase().contains(e))) {
                        log.info("未知的 RCPT: " + response + " email: " + email);
                    }
                }


                if (response != null && response.startsWith("250")) {
                    return new Result(email, true, "邮箱存在");
//                    Boolean b = waimaolangCheck(email);
//                    if (b == null || b) {
//                        return new Result(email, true, "邮箱存在");
//                    } else {
//                        return new Result(email, false, "邮箱不存在");
//                    }
                } else if (response != null && (noExists.stream().anyMatch(e -> response.toLowerCase().contains(e)))) {
                    return new Result(email, false, "邮箱不存在");
                } else if (response != null && (spamhaus.stream().anyMatch(e -> response.toLowerCase().contains(e)))) {
//                    return new Result(email, null, "IP进入黑名单");
                    Boolean b = waimaolangCheck(email);
                    log.info("waimaolangCheck end : {} -> {}", email, b);
                    if (b == null) {
                        return new Result(email, null, "IP进入黑名单");
                    } else if (b) {
                        return new Result(email, true, "邮箱存在");
                    } else {
                        return new Result(email, false, "邮箱不存在");
                    }
                } else if (response != null && others.stream().anyMatch(e -> response.toLowerCase().contains(e))) {
                    Boolean b = waimaolangCheck(email);
                    log.info("waimaolangCheck end : {} -> {}", email, b);
                    if (b == null) {
                        return new Result(email, null, "其他错误:" + response);
                    } else if (b) {
                        return new Result(email, true, "邮箱存在");
                    } else {
                        return new Result(email, false, "邮箱不存在");
                    }
                } else {
                    return new Result(email, null, "未知响应: " + response);
                }
            } catch (Exception e) {
                if (verbose) {
                    System.out.println("⚠️ SMTP检测错误 " + mx + ": " + e.getMessage());
                }
            }
        }
        return new Result(email, false, "所有MX服务器无法连接");
    }

    /** 批量检测 */
    public Map<String, List<Result>> checkBatchEmails(List<String> emails, int maxWorkers, int timeoutSeconds) throws InterruptedException {
//        long start = System.currentTimeMillis();
        log.info("checkBatchEmails start");
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(maxWorkers, emails.size()));
        Map<String, List<Result>> results = new HashMap<>();
        results.put("valid", new ArrayList<>());
        results.put("invalid", new ArrayList<>());
        results.put("unknown", new ArrayList<>());

        try {
            // 构建 Callable 列表，每个任务检查一个邮箱
            List<Callable<Result>> tasks = emails.stream()
                    .map(email -> (Callable<Result>) () -> {
                        try {
                            return checkSmtpConnection(email);
                        } catch (Exception e) {
                            return new Result(email, null, "执行异常");
                        }
                    })
                    .toList();

            // 批量提交并设置超时时间
            List<Future<Result>> futures = executor.invokeAll(tasks, timeoutSeconds, TimeUnit.SECONDS);

            for (int i = 0; i < futures.size(); i++) {
                Future<Result> f = futures.get(i);
                String email = emails.get(i);
                Result r;
                if (f.isCancelled()) {
                    r = new Result(email, null, "执行超时");
                } else {
                    try {
                        r = f.get(); // get 不会阻塞，因为 invokeAll 已经返回
                    } catch (ExecutionException e) {
                        r = new Result(email, null, "执行异常");
                    }
                }

                if (Boolean.TRUE.equals(r.valid)) {
                    results.get("valid").add(r);
                } else if (Boolean.FALSE.equals(r.valid)) {
                    results.get("invalid").add(r);
                } else {
                    results.get("unknown").add(r);
                }
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }

        log.info("checkBatchEmails end");
        return results;
    }

    /** 保存结果到 CSV */
    public void saveResultsToFile(Map<String, List<Result>> results, String outputFile) {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))) {
            pw.println("邮箱,状态,描述");
            for (Result r : results.get("valid")) {
                pw.println(r.email + ",有效," + r.message);
            }
            for (Result r : results.get("invalid")) {
                pw.println(r.email + ",无效," + r.message);
            }
            for (Result r : results.get("unknown")) {
                pw.println(r.email + ",未知," + r.message);
            }
        } catch (Exception e) {
            System.out.println("❌ 保存文件失败: " + e.getMessage());
        }
    }

    /** 内部类保存结果 */
    @Data
    public static class Result {
        String email;
        Boolean valid;
        String message;

        Result(String email, Boolean valid, String message) {
            this.email = email;
            this.valid = valid;
            this.message = message;
        }

        @Override
        public String toString() {
            return (valid == null ? "⚠️ " : valid ? "✅ " : "❌ ") + email + ": " + message;
        }
    }

    // https://vmail.waimaolang.cn/Validation.aspx?email=demartin.d%40ghc.org&type=mailbox
    public Boolean waimaolangCheck (String email) {
        if (redisUtil != null && !redisUtil.getLock("waimaolangCheck", "1", 1, TimeUnit.SECONDS)) {
            log.info("waimaolangCheck rate limit : {}", email);
            return null;
        }
        Socks5 socks5 = socks5Service.findCanUse();
        if (socks5 == null) {
            return null;
        }

        try {
            OkHttpClient defaultClient = OkHttpClientFactory.getProxyClient(socks5);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://vmail.waimaolang.cn/Validation.aspx?email="+email+"&type=mailbox");
            request = builder.get().addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
                    .addHeader("referer", "https://vmail.waimaolang.cn/")
                    .addHeader("host", "vmail.waimaolang.cn").build();

            log.info("waimaolangCheck start : {}", email);
            try (Response response = defaultClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String respStr = response.body().string();
                    log.info("waimaolangCheck result : {} -> {}", email, respStr);
                    JSONArray array = JSON.parseObject(respStr).getJSONArray("data");
                    for (int i = 0; i < array.size(); i++) {
                        if (array.getJSONObject(i).getString("module").equals("邮箱验证")) {
                            return array.getJSONObject(i).getString("msg").equals("邮箱验证成功");
                        }
                    }
                    return false;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** main 方法示例 */
    public static void main(String[] args) throws Exception {
        EmailChecker checker = new EmailChecker(10, true, null, null);
        List<String> emails = Arrays.asList("test@qq.com", "fake_email@example.com");
        Map<String, List<Result>> results = checker.checkBatchEmails(emails, 5, 60);
        checker.saveResultsToFile(results, "results.csv");
    }
}

