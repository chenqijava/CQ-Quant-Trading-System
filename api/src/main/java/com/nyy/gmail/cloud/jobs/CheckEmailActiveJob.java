package com.nyy.gmail.cloud.jobs;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.SubTaskStatusEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.service.ParamsService;
import com.nyy.gmail.cloud.service.Socks5Service;
import com.nyy.gmail.cloud.tasks.impl.EmailCheckActiveTaskImpl;
import com.nyy.gmail.cloud.utils.EmailChecker;
import com.nyy.gmail.cloud.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CheckEmailActiveJob {

    private volatile boolean is_running = false;

    private volatile boolean is_running_other = false;

    @Autowired
    @Qualifier("CheckEmailActiveExecutor")
    private Executor executor;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private EmailCheckActiveTaskImpl emailCheckActiveTask;

    @Autowired
    private ParamsService paramsService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private Socks5Service socks5Service;

    @Value("${application.taskType}")
    private String taskType;

//    @Async("other")
//    @Scheduled(cron = "0/2 * * * * ?")
    public void run () {
        if (taskType.equals("googleai")) {
            return;
        }
        if (is_running) return;


        // 添加开关
        String useThirdGmailCheck = paramsService.getParams("account.useThirdGmailCheck", null, null).toString();
        if (useThirdGmailCheck.equalsIgnoreCase("NO")) {
            return;
        }

        is_running =  true;

        try {
            PageResult<SubTask> subTaskPageResult = subTaskRepository.findByPagination(100, 1, Map.of("type", TaskTypesEnums.EmailCheckActive.getCode(),
                    "status", Map.of("$in", List.of(SubTaskStatusEnums.init.getCode(), SubTaskStatusEnums.processing.getCode()))), null);
            if (subTaskPageResult != null && subTaskPageResult.getData() != null && !subTaskPageResult.getData().isEmpty()) {
                log.info("start CheckEmailActiveJob");
                Map<String, String> gmailMap = new ConcurrentHashMap<>();
                for (SubTask subTask : subTaskPageResult.getData()) {
                    String email = subTask.getParams().getOrDefault("email", "").toString();
                    if (email.toLowerCase().endsWith("@gmail.com")) {
                        gmailMap.put(email, subTask.get_id());
                    }
                }
                if (gmailMap.isEmpty()) {
                    return;
                }
                // 调用接口
                OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
                // JSON 请求体
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                String jsonBody = JSON.toJSONString(Map.of(
                        "token", "eb60777a4dc055e0d22b338add5d5ac2", "mails", gmailMap.keySet(), "mailtype", 0));

                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, mediaType);

                Request request = null;
                Request.Builder builder = new Request.Builder().url("https://eric3366888.gmailcheck.com/api.php");

                request = builder.header("Content-Type", "application/json").header("token", "eb60777a4dc055e0d22b338add5d5ac2")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36").post(body).build();

                try (Response response = defaultClient.newCall(request).execute()) {
                    if (response.code() == 200 && response.body() != null) {
                        String respStr = response.body().string();

                        JSONObject jsonObject = JSON.parseObject(respStr);
                        if (jsonObject.getString("status").equals("success")) {
                            JSONArray jsonArray = jsonObject.getJSONArray("goodlist");
                            if (jsonArray != null) {
                                for (Object o : jsonArray) {
                                    String id = gmailMap.get(o.toString());
                                    subTaskRepository.updateStatusAndResultById(id, SubTaskStatusEnums.success.getCode(), "邮箱存在");
                                }
                            }
                            jsonArray = jsonObject.getJSONArray("badlist");
                            if (jsonArray != null) {
                                for (Object o : jsonArray) {
                                    String id = gmailMap.get(o.toString());
                                    subTaskRepository.updateStatusAndResultById(id, SubTaskStatusEnums.success.getCode(), "邮箱异常");
                                }
                            }
                            jsonArray = jsonObject.getJSONArray("otherlist");
                            if (jsonArray != null) {
                                for (Object o : jsonArray) {
                                    String id = gmailMap.get(o.toString());
                                    subTaskRepository.updateStatusAndResultById(id, SubTaskStatusEnums.success.getCode(), "邮箱无法检测，不存在");
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    log.error("Request failed error: {}", e.getMessage());
                }
                log.info("end CheckEmailActiveJob");
            }
        } finally {
//            log.info("end CheckEmailActiveJob");
            is_running = false;
        }
    }

//    @Async("other")
//    @Scheduled(cron = "0/1 * * * * ?")
    public void runOtherEmail () {
        if (is_running_other) return;
        is_running_other =  true;

        // 添加开关
        String useThirdGmailCheck = paramsService.getParams("account.useThirdGmailCheck", null, null).toString();
        String useThreadNumGmailCheck = paramsService.getParams("account.useThreadNumGmailCheck", null, null).toString();
        String onceCheckTimeout = paramsService.getParams("account.onceCheckTimeout", null, null).toString();

        try {
            PageResult<SubTask> subTaskPageResult = subTaskRepository.findByPagination(Integer.parseInt(useThreadNumGmailCheck), 1, Map.of("type", TaskTypesEnums.EmailCheckActive.getCode(),
                    "status", Map.of("$in", List.of(SubTaskStatusEnums.init.getCode(), SubTaskStatusEnums.processing.getCode()))), null);
            if (subTaskPageResult != null && subTaskPageResult.getData() != null && !subTaskPageResult.getData().isEmpty()) {
                log.info("start CheckEmailActiveJob runOtherEmail");
                Map<String, List<String>> otherMap = new ConcurrentHashMap<>();

                for (SubTask subTask : subTaskPageResult.getData()) {
                    String email = subTask.getParams().getOrDefault("email", "").toString();

                    // 判断是否需要过滤非 Gmail
                    if (useThirdGmailCheck.equalsIgnoreCase("NO") ||
                            !email.toLowerCase().endsWith("@gmail.com")) {

                        // 使用 computeIfAbsent 保证线程安全地初始化 List
                        otherMap.computeIfAbsent(email, k -> Collections.synchronizedList(new ArrayList<>()))
                                .add(subTask.get_id());
                    }
                }

                EmailChecker checker = new EmailChecker(5, true, redisUtil, socks5Service);
                try {
                    Map<String, List<EmailChecker.Result>> map = checker.checkBatchEmails(otherMap.keySet().stream().toList(), Integer.parseInt(useThreadNumGmailCheck), Integer.valueOf(onceCheckTimeout));
                    List<String> ids = new ArrayList<>();
                    for (EmailChecker.Result valid : map.get("valid")) {
                        List<String> id = otherMap.get(valid.getEmail());
                        ids.addAll(id);
                    }
                    if (!ids.isEmpty()) {
                        subTaskRepository.updateStatusAndResultByIds(ids, SubTaskStatusEnums.success.getCode(), "邮箱存在");
                    }
                    ids = new ArrayList<>();
                    for (EmailChecker.Result valid : map.get("invalid")) {
                        List<String> id = otherMap.get(valid.getEmail());
                        ids.addAll(id);
                    }
                    if (!ids.isEmpty()) {
                        subTaskRepository.updateStatusAndResultByIds(ids, SubTaskStatusEnums.success.getCode(), "邮箱异常");
                    }
                    Map<String, List<String>> map2 = new HashMap<>();
                    for (EmailChecker.Result valid : map.get("unknown")) {
                        List<String> id = otherMap.get(valid.getEmail());
                        List<String> id2 = map2.getOrDefault(valid.getMessage(), new ArrayList<>());
                        id2.addAll(id);
                        map2.put(valid.getMessage(), id2);
                    }
                    for (String message : map2.keySet()) {
                        ids = map2.get(message);
                        subTaskRepository.updateStatusAndResultByIds(ids, SubTaskStatusEnums.failed.getCode(), message);
                    }
                } catch (InterruptedException e) {
                    log.error("中断检查批量发送邮件：" + e.getMessage());
                }

//                // 调用接口
//                OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
//                // JSON 请求体
//                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
//                String jsonBody = JSON.toJSONString(Map.of(
//                        "token", "eb60777a4dc055e0d22b338add5d5ac2", "mails", otherMap.keySet(), "mailtype", 0));
//
//                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, mediaType);
//
//                Request request = null;
//                Request.Builder builder = new Request.Builder().url("https://google9picverify.tnt-pub.com/api/emailCheck");
//
//                request = builder.header("Content-Type", "application/json").header("token", "eb60777a4dc055e0d22b338add5d5ac2")
//                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36").post(body).build();
//
//                try (Response response = defaultClient.newCall(request).execute()) {
//                    if (response.code() == 200 && response.body() != null) {
//                        String respStr = response.body().string();
//
//                        JSONObject jsonObject = JSON.parseObject(respStr);
//                        if (jsonObject.getString("status").equals("success")) {
//                            JSONArray jsonArray = jsonObject.getJSONArray("goodlist");
//                            if (jsonArray != null) {
//                                for (Object o : jsonArray) {
//                                    String id = otherMap.get(o.toString());
//                                    subTaskRepository.updateStatusAndResultById(id, SubTaskStatusEnums.success.getCode(), "邮箱存在");
//                                }
//                            }
//                            jsonArray = jsonObject.getJSONArray("badlist");
//                            if (jsonArray != null) {
//                                for (Object o : jsonArray) {
//                                    String id = otherMap.get(o.toString());
//                                    subTaskRepository.updateStatusAndResultById(id, SubTaskStatusEnums.success.getCode(), "邮箱异常");
//                                }
//                            }
//                            jsonArray = jsonObject.getJSONArray("otherlist");
//                            if (jsonArray != null) {
//                                for (Object o : jsonArray) {
//                                    subTaskRepository.updateStatusAndResultById(o.toString(), SubTaskStatusEnums.success.getCode(), "邮箱无法检测，不存在");
//                                }
//                            }
//                        }
//                    }
//                } catch (IOException e) {
//                    log.error("Request failed error: {}", e.getMessage());
//                }
                log.info("end CheckEmailActiveJob");
//                int size = otherMap.size();
//                if (size == 0) {
//                    return;
//                }
//                CountDownLatch latch = new CountDownLatch(size);
//                for (Map.Entry<String, String> entry : otherMap.entrySet()) {
//                    String key = entry.getKey();
//                    String value = entry.getValue();
//                    executor.execute(() -> {
//                        try {
//                            String checked = emailCheckActiveTask.checkEmail(key);
//                            subTaskRepository.updateStatusAndResultById(value, SubTaskStatusEnums.success.getCode(), checked);
//                        } catch (Throwable e) {
//                            subTaskRepository.updateStatusAndResultById(value, SubTaskStatusEnums.failed.getCode(), "任务执行错误：" + e.getMessage());
//                        }finally {
//                            latch.countDown();
//                        }
//                    });
//                }
//                latch.await(60, TimeUnit.SECONDS);

                log.info("end CheckEmailActiveJob runOtherEmail");
            }
        } finally {
            is_running_other = false;
        }
    }
}
