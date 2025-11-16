package com.nyy.gmail.cloud.controller;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.entity.mysql.ApiKey;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.model.vo.UploadFileVO;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.repository.mysql.ApiKeyRepository;
import com.nyy.gmail.cloud.service.*;
import com.nyy.gmail.cloud.service.ai.AiImageRecognition;
import com.nyy.gmail.cloud.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping({"/api/open"})
public class OpenApiController {

    private static List<String> googleModels = List.of("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash");

    private static List<String> chatgptModels = List.of("auto", "gpt-5", "gpt-5-mini", "gpt-4o", "gpt-4o-mini");

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private ParamsService paramsService;

    @Value("${application.taskType}")
    private String taskType;

    @Autowired
    private GoogleAiServerService googleAiServerService;

    @Autowired
    private TaskServerRecordRepository taskServerRecordRepository;

    @Autowired
    private GoogleStudioApiKeyRepository googleStudioApiKeyRepository;

    @Autowired
    private Socks5Service socks5Service;

    @Autowired
    private ProxyAccountService proxyAccountService;

    @Autowired
    private Socks5Repository socks5Repository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private EmailCheckActiveService emailCheckActiveService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserApiKeyGeminiCallInfoRepository userApiKeyGeminiCallInfoRepository;

    @Autowired
    private UserApiKeyTokenStatisticsRepository userApiKeyTokenStatisticsRepository;

    @Autowired
    private AiModelPriceRepository aiModelPriceRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceDetailRepository balanceDetailRepository;

    // 提交任务
    @PostMapping("imageRecognition")
    public Result<ImageRecognitionRespDto> imageRecognition(@RequestBody(required = false) ImageRecognitionReqDto reqDto) {
        if (taskType.equals("googleai")) {
            if (CollectionUtils.isEmpty(reqDto.getLinkList())) {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID);
            }
            if (reqDto.getLinkList().size() > 5000) {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID, "单次数量不能超过5000");
            }
            AccountListDTO accountListDTO = new AccountListDTO();
            accountListDTO.setFilters(new HashMap<>());
            accountListDTO.getFilters().put("onlineStatus", AccountOnlineStatus.ONLINE.getCode());
            accountListDTO.getFilters().put("userID", Session.currentSession().userID);

            List<SubTask> subTasks = subTaskRepository.findByStatus(SubTaskStatusEnums.init.getCode());
            List<String> useIds = subTasks.stream().map(SubTask::getAccid).toList();
            accountListDTO.getFilters().put("_id", Map.of("$nin", useIds));

            PageResult<Account> pagination = accountRepository.findByPagination(accountListDTO, reqDto.getLinkList().size(), 1);
            if (CollectionUtils.isEmpty(pagination.getData())) {
                throw new CommonException(ResultCode.NO_ONLINE_ACCOUNT);
            }
            int onceImageNum = Integer.parseInt(paramsService.getParams("account.onceImageNum", null, null).toString());
            if (onceImageNum <= 1) {
                onceImageNum = 3;
            }

            List<String> ids = pagination.getData().stream().map(Account::get_id).collect(Collectors.toList());
            Collections.shuffle(ids); // 随机打乱
            int sizeToPick = ids.size() / onceImageNum + onceImageNum; // 取 1/3，向下取整
            if (ids.size() > sizeToPick) {
                ids = ids.subList(0, sizeToPick);
            }


            Map<String, Object> params = new HashMap<>();
            params.put("addMethod", "1");
            params.put("addDatas", reqDto.getLinkList());
            if (StringUtils.isNotEmpty(reqDto.getPromptVersion())) {
                params.put("promptVersion", reqDto.getPromptVersion());
            }
            if (StringUtils.isNotEmpty(reqDto.getProjectName())) {
                params.put("projectName", reqDto.getProjectName());
            }

            GroupTask groupTask = taskUtil.createGroupTask(ids, TaskTypesEnums.ImageRecognition, params, Session.currentSession().userID);
            ImageRecognitionRespDto imageRecognitionRespDto = new ImageRecognitionRespDto();
            imageRecognitionRespDto.setTaskId(groupTask.get_id());
            return ResponseResult.success(imageRecognitionRespDto);
        } else {
            if (CollectionUtils.isEmpty(reqDto.getLinkList())) {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID);
            }
            if (reqDto.getLinkList().size() > 5000) {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID, "单次数量不能超过5000");
            }
            GoogleAiServer googleAiServer = googleAiServerService.selectOneServer(false);
            if (googleAiServer == null) {
                throw new  CommonException(ResultCode.NO_ONLINE_ACCOUNT, "没有可用服务器");
            }
            OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
            // JSON 请求体
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String jsonBody = JSON.toJSONString(reqDto);

            okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url(googleAiServer.getUrl() + "/api/open/imageRecognition");
            builder.addHeader("X-API-KEY", googleAiServer.getApiKey());
            builder.addHeader("X-SIGN", SignGenerator.generateSign(JSON.parseObject(jsonBody, Map.class), googleAiServer.getApiSecret()));
            request = builder.post(body).build();

            try (Response response = defaultClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Result result = JSON.parseObject(respStr, Result.class);
                    JSONObject jsonObject = JSONObject.parseObject(respStr);
                    jsonObject = jsonObject.getJSONObject("data");
                    String taskId = jsonObject.getString("taskId");

                    TaskServerRecord taskServerRecord = new TaskServerRecord();
                    taskServerRecord.setTaskId(taskId);
                    taskServerRecord.setCreateTime(new Date());
                    taskServerRecord.setServerId(googleAiServer.get_id());
                    taskServerRecord.setServer(googleAiServer);
                    taskServerRecordRepository.save(taskServerRecord);
                    return result;
                } else {
                    throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, "请求接口失败");
                }
            } catch (IOException e) {
                log.error("Request failed error: {}", e.getMessage());
                throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
    }

    // 查询任务
    @PostMapping("imageRecognitionDetail")
    public Result<PageResult<ImageRecognitionDetailRespDto>> imageRecognitionDetail(@RequestBody(required = false) ImageRecognitionDetailReqDto reqDto) {
        String userID = Session.currentSession().userID;
        if (taskType.equals("googleai")) {
            Map<String, Object> filters = new HashMap<>(Map.of("groupTaskId", reqDto.getTaskId(), "type", TaskTypesEnums.ImageRecognition.getCode(), "userID", userID));
            if (StringUtils.isNotEmpty(reqDto.getStatus())) {
                filters.put("status", reqDto.getStatus());
            }
            PageResult<SubTask> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                    .builder(SubTask.class)
                    .filters(filters)
                    .sorter(Map.of("_id", -1, "createTime", -1))
                    .pageSize(reqDto.getPageSize())
                    .page(reqDto.getPageNum())
                    .build());
            PageResult<ImageRecognitionDetailRespDto> dtoPageResult = new PageResult<>();
            dtoPageResult.setPageSize(pageResult.getPageSize());
            dtoPageResult.setPages(pageResult.getPages());
            dtoPageResult.setPageNum(pageResult.getPageNum());
            dtoPageResult.setTotal(pageResult.getTotal());
            dtoPageResult.setData(pageResult.getData().stream().map(e -> {
                ImageRecognitionDetailRespDto imageRecognitionDetailRespDto = new ImageRecognitionDetailRespDto();
                imageRecognitionDetailRespDto.setId(e.get_id());
                imageRecognitionDetailRespDto.setStatus(e.getStatus());
                imageRecognitionDetailRespDto.setResult(e.getResult());
                imageRecognitionDetailRespDto.setParams(e.getParams());
                imageRecognitionDetailRespDto.setCreateTime(e.getCreateTime());
                imageRecognitionDetailRespDto.setFinishTime(e.getFinishTime());
                imageRecognitionDetailRespDto.setTaskId(e.getGroupTaskId());
                return imageRecognitionDetailRespDto;
            }).toList());
            return ResponseResult.success(dtoPageResult);
        } else {
            TaskServerRecord record = taskServerRecordRepository.findByTaskId(reqDto.getTaskId());
            if (record != null) {
                GoogleAiServer googleAiServer = googleAiServerService.findById(record.getServerId());
                if (googleAiServer == null) {
                    googleAiServer = record.getServer();
                }
                if (googleAiServer == null) {
                    throw new  CommonException(ResultCode.NO_ONLINE_ACCOUNT, "没有可用服务器");
                }
                OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
                // JSON 请求体
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                String jsonBody = JSON.toJSONString(reqDto);

                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, mediaType);

                Request request = null;
                Request.Builder builder = new Request.Builder().url(googleAiServer.getUrl() + "/api/open/imageRecognitionDetail");
                builder.addHeader("X-API-KEY", googleAiServer.getApiKey());
                builder.addHeader("X-SIGN", SignGenerator.generateSign(JSON.parseObject(jsonBody, Map.class), googleAiServer.getApiSecret()));
                request = builder.post(body).build();

                try (Response response = defaultClient.newCall(request).execute()) {
                    if (response.code() == 200) {
                        String respStr = response.body().string();
                        return JSON.parseObject(respStr, Result.class);
                    } else {
                        if (response.body() != null) {
                            throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, "code: " + response.code() + " response: " + response.body().string());
                        } else {
                            throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, "请求接口失败");
                        }
                    }
                } catch (IOException e) {
                    log.error("Request failed error: {}", e.getMessage());
                    throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, e.getMessage());
                }
            } else {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID);
            }
        }
    }

    // 查询子任务
//    @PostMapping("imageRecognitionDetailSub")
//    public Result<ImageRecognitionDetailRespDto> imageRecognitionDetailSub(@RequestBody(required = false) IdsListDTO reqDto) {
//        String userID = Session.currentSession().userID;
//
//        SubTask subTask = subTaskRepository.findById(reqDto.getId());
//        if (subTask == null || !subTask.getUserID().equals(userID)) {
//            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
//        }
//
//        ImageRecognitionDetailRespDto imageRecognitionDetailRespDto = new ImageRecognitionDetailRespDto();
//        imageRecognitionDetailRespDto.setId(subTask.get_id());
//        imageRecognitionDetailRespDto.setStatus(subTask.getStatus());
//        imageRecognitionDetailRespDto.setResult(subTask.getResult());
//        imageRecognitionDetailRespDto.setParams(subTask.getParams());
//        imageRecognitionDetailRespDto.setCreateTime(subTask.getCreateTime());
//        imageRecognitionDetailRespDto.setFinishTime(subTask.getFinishTime());
//        imageRecognitionDetailRespDto.setTaskId(subTask.getGroupTaskId());
//        return ResponseResult.success(imageRecognitionDetailRespDto);
//    }


    @PostMapping("imageRecognitionTask")
    public Result<Map<String, String>> imageRecognitionTask(@RequestBody(required = false) IdsListDTO reqDto) {
        String userID = Session.currentSession().userID;

        if (taskType.equals("googleai")) {
            GroupTask groupTask = groupTaskRepository.findById(reqDto.getId()).orElse(null);
            if (groupTask == null || !groupTask.getUserID().equals(userID)) {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID);
            }
            Map<String, String> map = new HashMap<>();
            map.put("id", groupTask.get_id());
            map.put("status", groupTask.getStatus());
            return ResponseResult.success(map);
        } else {
            TaskServerRecord record = taskServerRecordRepository.findByTaskId(reqDto.getId());
            if (record != null) {
                GoogleAiServer googleAiServer = googleAiServerService.findById(record.getServerId());
                if (googleAiServer == null) {
                    googleAiServer = record.getServer();
                }
                if (googleAiServer == null) {
                    throw new  CommonException(ResultCode.NO_ONLINE_ACCOUNT, "没有可用服务器");
                }
                OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
                // JSON 请求体
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                String jsonBody = JSON.toJSONString(reqDto);

                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, mediaType);

                Request request = null;
                Request.Builder builder = new Request.Builder().url(googleAiServer.getUrl() + "/api/open/imageRecognitionTask");
                builder.addHeader("X-API-KEY", googleAiServer.getApiKey());
                builder.addHeader("X-SIGN", SignGenerator.generateSign(JSON.parseObject(jsonBody, Map.class), googleAiServer.getApiSecret()));
                request = builder.post(body).build();

                try (Response response = defaultClient.newCall(request).execute()) {
                    if (response.code() == 200) {
                        String respStr = response.body().string();
                        return JSON.parseObject(respStr, Result.class);
                    } else {
                        if (response.body() != null) {
                            throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, "code: " + response.code() + " response: " + response.body().string());
                        } else {
                            throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, "请求接口失败");
                        }
                    }
                } catch (IOException e) {
                    log.error("Request failed error: {}", e.getMessage());
                    throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, e.getMessage());
                }
            } else {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID);
            }
        }
    }

    @PostMapping("serverScore")
    public Result<Map<String, Double>> serverScore() {
        long l = subTaskRepository.countStatusIn(List.of(SubTaskStatusEnums.init.getCode(), SubTaskStatusEnums.processing.getCode()));
        long l2= subTaskRepository.countStatusInRecent(List.of(SubTaskStatusEnums.success.getCode(), SubTaskStatusEnums.failed.getCode()), 1);
        double score = 0.7 * (1.0 - Math.min(l, 10000) / 10000.0) + 0.3 * Math.min(l2, 2000) / 2000.0;
        List<GoogleStudioApiKey> enable = googleStudioApiKeyRepository.findByStatus("enable");
        List<GoogleStudioApiKey> googleEnable = googleStudioApiKeyRepository.findByStatusAndType("enable", AiTypeEnums.GoogleStudio.getCode());
        List<GoogleStudioApiKey> chatgptEnable = googleStudioApiKeyRepository.findByStatusAndType("enable", AiTypeEnums.Chatgpt.getCode());
        Map<String, Double> map = new HashMap<>();
        map.put("score", score);
        map.put("throughput", l2 * 1.0);
        map.put("noImages", l * 10.0);
        map.put("enableKeyNum", enable.size() * 1.0);
        map.put("enableKeyGoogleNum", googleEnable.size() * 1.0);
        map.put("enableKeyChatgptNum", chatgptEnable.size() * 1.0);
        return ResponseResult.success(map);
    }

    @PostMapping({"serverList"})
    public Result<List<GoogleAiServer>> serverList() {
        PageResult<GoogleAiServer> query = mongoPaginationHelper.query(
                MongoPaginationBuilder.builder(GoogleAiServer.class).
                        filters(null)
                        .sorter(null)
                        .pageSize(1000).page(1).build());
        return ResponseResult.success(query.getData());
    }

    @NoLogin
    @PostMapping({"/v1beta/models/{model}"})
    public Map generateContent(@RequestParam("key") String apiKey, @PathVariable("model") String model, @RequestBody(required = false) Map<String, Object> params) {
        User user = null;
        if (!apiKey.equals("AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE")) {
            user = userService.findUserByUserApiKey(apiKey);
            if (user == null) {
                ApiKey byApiKey = apiKeyRepository.findByApiKey(apiKey);
                if (byApiKey == null || !byApiKey.getUserID().equals(Constants.ADMIN_USER_ID)) {
                    return new HashMap(Map.of("code", ResultCode.PARAMS_IS_INVALID, "error", "key is invalid"));
                }
            } else {
                if (user.getStatus().equals(UserStatusEnum.DISABLED.getCode())) {
                    return new HashMap(Map.of("code", ResultCode.ACCOUNT_BANNED, "error", ResultCode.ACCOUNT_BANNED.getMessage()));
                }
//                if (user.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
//                    return new HashMap(Map.of("code", ResultCode.USER_BALANCE_INSUFFICIENT, "error", ResultCode.USER_BALANCE_INSUFFICIENT.getMessage()));
//                }
            }
        }
        model = model.split(":", -1)[0];
        AiModelPrice modelPrice = aiModelPriceRepository.findByModel(model);
        if (user != null && (modelPrice == null || !modelPrice.getStatus().equals("enable"))) {
//            return Map.of("error",
//                    Map.of("code", 404,
//                            "message", "models/"+model+" is not found for API version v1beta, or is not supported for generateContent. Call ListModels to see the list of available models and their supported methods.",
//                            "status", "NOT_FOUND"));
            return new HashMap(Map.of("code", ResultCode.NO_SUPPORT_MODEL, "error", ResultCode.NO_SUPPORT_MODEL.getMessage() + ":" + model));
        }
        if (user != null && user.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return new HashMap(Map.of("code", ResultCode.USER_BALANCE_INSUFFICIENT, "error", ResultCode.USER_BALANCE_INSUFFICIENT.getMessage()));
        }
        if (taskType.equals("googleai")) {
            GoogleStudioApiKey googleStudioApiKey = null;
            String responseStr = "";
            for (int i = 0; i < 10; i++) {
                int waitInterval = 10;
                try {
                    List<GoogleStudioApiKey> googleStudioApiKeyList = googleStudioApiKeyRepository.findByCanUsed(100, Constants.ADMIN_USER_ID, null, AiTypeEnums.GoogleStudio)
                            .stream().filter(e -> e.getUseByDay() < e.getLimitByDay() && e.getUseByMinute() < e.getLimitByMinute()).collect(Collectors.toList());
                    if (googleStudioApiKeyList.isEmpty()) {
                        return Map.of("code", ResultCode.INTERNAL_SERVER_ERROR.getCode(), "error", "没有可用的余额");
                    }
                    // 随机取一个
                    Collections.shuffle(googleStudioApiKeyList);
                    googleStudioApiKey = googleStudioApiKeyList.getFirst();

                    if (i > 0) {
                        socks5Service.releaseSocks5(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null);
                        googleStudioApiKey.setSocks5Id("");
                    }
                    Socks5 socks5 = null;
                    if (StringUtils.isEmpty(googleStudioApiKey.getSocks5Id())) {
                        socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                        if (socks5 == null) {
                            throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                        }
                        googleStudioApiKey.setSocks5Id(socks5.get_id());
                        googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
                    } else {
                        socks5 = socks5Repository.findSocks5ById(googleStudioApiKey.getSocks5Id());
                        if (socks5 == null) {
                            socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                            if (socks5 == null) {
                                throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                            }
                            googleStudioApiKey.setSocks5Id(socks5.get_id());
                            googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
                        }
                    }
                    if (socks5 == null) {
                        throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                    }

                    // 更新次数
                    for (int j = 0; j < 10; j++) {
                        try {
                            googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                            googleStudioApiKey.setUseByMinute(googleStudioApiKey.getUseByMinute() + 1);
                            googleStudioApiKey.setUseByDay(googleStudioApiKey.getUseByDay() + 1);
                            googleStudioApiKeyRepository.update(googleStudioApiKey);
                            break;
                        } catch (Exception e) {
                            log.info("ImageRecognition ERROR update 1: {}", e.getMessage());
                        }
                    }

                    String type = AiTypeEnums.GoogleStudio.getCode();
                    if (StringUtils.isNotEmpty(googleStudioApiKey.getType())) {
                        type = googleStudioApiKey.getType();
                    }

                    AiImageRecognition imageRecognition = applicationContext.getBean(type, AiImageRecognition.class);
                    String contentOptimize = imageRecognition.proxyMode(socks5, googleStudioApiKey.getApiKey(), JSON.toJSONString(params), model);
                    if (StringUtils.isNotEmpty(contentOptimize)) {
                        // 成功使用识别
                        for (int j = 0; j < 10; j++) {
                            try {
                                googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                                googleStudioApiKey.setUsedSuccess(googleStudioApiKey.getUsedSuccess() + 1);
                                googleStudioApiKey.setUsedSuccessByDay(googleStudioApiKey.getUsedSuccessByDay() + 1);
                                Date lastSuccessTime = googleStudioApiKey.getLastSuccessTime();
                                if (lastSuccessTime != null) {
                                    googleStudioApiKey.setTwoSuccessInterval((int) ((new Date().getTime() - lastSuccessTime.getTime()) / 1000));
                                }
                                googleStudioApiKey.setLastSuccessTime(new Date());
                                googleStudioApiKey.setConsecutiveFailureCount(0);
                                googleStudioApiKeyRepository.update(googleStudioApiKey);
                                break;
                            } catch (Exception e) {
                                log.info("ImageRecognition ERROR update 2: {}", e.getMessage());
                            }
                        }
                        saveCallInfo(params, contentOptimize, user, googleStudioApiKey.getApiKey());
                        Map<String, Long> map = saveTokenStatistics(user, contentOptimize, modelPrice);
//                        Long promptTokenCount = map.get("promptTokenCount");
//                        Long candidatesTokenCount = map.get("candidatesTokenCount");
//                        Long totalTokenCount = map.get("totalTokenCount");

                        return JSON.parseObject(contentOptimize, Map.class);
                    }
                } catch (Exception e) {
                    responseStr = e.getMessage();
                    if (googleStudioApiKey != null) {
                        // 释放IP
                        socks5Service.releaseSocks5(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null);
                        googleStudioApiKey.setSocks5Id("");

                        // if (e instanceof CommonException && (e.getMessage().contains("403") || e.getMessage().equals("API key not valid"))) {
                        if (e instanceof CommonException && (e.getMessage().contains("403") || e.getMessage().contains("API key not valid") || e.getMessage().contains("User location is not supported for the API use."))) {
                            googleStudioApiKeyRepository.updateDisable(googleStudioApiKey.get_id());
                        } else if (e instanceof CommonException && (e.getMessage().contains("429"))) {
                            waitInterval = 60*60; // 10分钟不可用
                        } else {
                            return Map.of("code", ResultCode.INTERNAL_SERVER_ERROR.getCode(), "error", e.getMessage());
                        }
                    }
                } finally {
                    if (googleStudioApiKey != null) {
                        String socks5Id = googleStudioApiKey.getSocks5Id();
                        googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                        if (googleStudioApiKey != null) {
                            if (waitInterval >= 60 * 60) {
                                int tooManyWaitSecond = Integer.parseInt(paramsService.getParams("account.tooManyWaitSecond", null, null).toString());

                                if (googleStudioApiKey.getConsecutiveFailureCount() == null) {
                                    googleStudioApiKey.setConsecutiveFailureCount(1);
                                } else {
                                    googleStudioApiKey.setConsecutiveFailureCount(googleStudioApiKey.getConsecutiveFailureCount() + 1);
                                }
                                waitInterval = tooManyWaitSecond * googleStudioApiKey.getConsecutiveFailureCount();
                            }
                            // 修改google下次使用时间
                            Calendar instance = Calendar.getInstance();
                            instance.add(Calendar.SECOND, waitInterval);
                            googleStudioApiKey.setNextUseTime(instance.getTime());
                            googleStudioApiKey.setSocks5Id(socks5Id);

                            googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), googleStudioApiKey.getNextUseTime(), googleStudioApiKey.getConsecutiveFailureCount());
                        }
                    }
                }
            }
            try {
                return JSON.parseObject(responseStr, Map.class);
            } catch (Exception e) {}
        } else {
            // 获取txt
            // contents/[0]/parts/[xxx]/text
            String jsonBody = JSON.toJSONString(params);
            String md5 = MD5Util.MD5(jsonBody);
            Object o = redisUtil.get("REQ_LIMIT:" + md5);
            if (o != null) {
                if (Integer.valueOf(o.toString()) >= 15) {
                    throw new CommonException(ResultCode.TOO_MANY_REQUEST, "请求太频繁");
                }
                redisUtil.set("REQ_LIMIT:" + md5, Integer.valueOf(o.toString()) + 1 + "", 60);
            } else {
                redisUtil.set("REQ_LIMIT:" + md5, "1", 60);
            }

            for (int i = 0; i < 10; i++) {
                GoogleAiServer googleAiServer = googleAiServerService.selectOneServer(true);
                if (googleAiServer == null) {
                    throw new CommonException(ResultCode.NO_ONLINE_ACCOUNT, "没有可用服务器");
                }
                OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
                // JSON 请求体
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");


                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, mediaType);

                Request request = null;
                Request.Builder builder = new Request.Builder().url(googleAiServer.getUrl() + "/api/open/v1beta/models/" + model + ":generateContent?key=AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE");
                //            builder.addHeader("X-API-KEY", googleAiServer.getApiKey());
                //            builder.addHeader("X-SIGN", SignGenerator.generateSign(JSON.parseObject(jsonBody, Map.class), googleAiServer.getApiSecret()));
                request = builder.post(body).build();

                try (Response response = defaultClient.newCall(request).execute()) {
                    if (response.body() != null) {
                        String respStr = response.body().string();
                        if (!respStr.contains("没有可用的余额")) {
                            if (user != null) {
                                saveCallInfo(params, respStr, user, "");
                                saveTokenStatistics(user, respStr, modelPrice);
                            }

                            return JSON.parseObject(respStr, Map.class);
                        }
                    } else {
                        throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, "请求超时，请重试");
                    }
                } catch (IOException e) {
                    log.error("Request failed error: {}", e.getMessage());
                    throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, e.getMessage());
                }
            }
        }

        return Map.of("code", ResultCode.INTERNAL_SERVER_ERROR.getCode(), "error", ResultCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    private Map<String, Long> saveTokenStatistics(User user, String respStr, AiModelPrice modelPrice) {
        Long promptTokenCount = 0L;
        Long candidatesTokenCount = 0L;
        Long totalTokenCount = 0L;
        String model = "";
        try {
            JSONObject jsonObject = JSON.parseObject(respStr);
            JSONObject metadata = jsonObject.getJSONObject("usageMetadata");
            promptTokenCount = metadata.getLong("promptTokenCount");
            candidatesTokenCount = metadata.getLong("candidatesTokenCount");
            totalTokenCount = metadata.getLong("totalTokenCount");
            model = jsonObject.getString("modelVersion");
        } catch (Exception e) {}

        if (promptTokenCount == null || promptTokenCount == 0L) {
            return Map.of("promptTokenCount", 0L, "candidatesTokenCount", 0L, "totalTokenCount", 0L);
        }

        if (user == null) {
            return Map.of("promptTokenCount", promptTokenCount, "candidatesTokenCount", candidatesTokenCount, "totalTokenCount", totalTokenCount);
        }
        UserApiKeyTokenStatistics userApiKey = userApiKeyTokenStatisticsRepository.findOneByUserApiKey(user.getUserApiKey());

        for (int i = 0; i < 10; i++) {
            try {
                if (userApiKey == null) {
                    userApiKey = new UserApiKeyTokenStatistics();
                    userApiKey.setApiKey(user.getUserApiKey());
                    userApiKey.setUserID(user.getUserID());
                    userApiKey.setCandidatesTokenCount(0L);
                    userApiKey.setPromptTokenCount(0L);
                    userApiKey.setTotalTokenCount(0L);
                    userApiKey.setCreateTime(new Date());
                    userApiKey.setCallCount(0L);
                    userApiKey.setOutputTokenCount(0L);
                }
                if (userApiKey.getTotalTokenCount() == null) {
                    userApiKey.setTotalTokenCount(0L);
                }
                if (userApiKey.getPromptTokenCount() == null) {
                    userApiKey.setPromptTokenCount(0L);
                }
                if (userApiKey.getCallCount() == null) {
                    userApiKey.setCallCount(0L);
                }
                if (userApiKey.getCandidatesTokenCount() == null) {
                    userApiKey.setCandidatesTokenCount(0L);
                }
                if (userApiKey.getOutputTokenCount() == null) {
                    userApiKey.setOutputTokenCount(userApiKey.getTotalTokenCount() - userApiKey.getPromptTokenCount());
                }
                userApiKey.setOutputTokenCount(userApiKey.getOutputTokenCount() + totalTokenCount - promptTokenCount);
                userApiKey.setPromptTokenCount(userApiKey.getPromptTokenCount() + promptTokenCount);
                userApiKey.setTotalTokenCount(userApiKey.getTotalTokenCount() + totalTokenCount);
                userApiKey.setCandidatesTokenCount(userApiKey.getCandidatesTokenCount() + candidatesTokenCount);
                userApiKey.setCallCount(userApiKey.getCallCount() + 1);
                userApiKeyTokenStatisticsRepository.save(userApiKey);
                break;
            } catch (Exception ex) {
                userApiKey = userApiKeyTokenStatisticsRepository.findOneByUserApiKey(user.getUserApiKey());
            }
        }

        BigDecimal price = calcPrice(user, promptTokenCount, candidatesTokenCount, totalTokenCount, model);

        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            RLock lock = redissonClient.getLock(user.getUserID() + "-charge");
            try {
                if (lock.tryLock(30, TimeUnit.SECONDS)) {
                    try {
                        user = userRepository.findOneByUserID(user.getUserID());
                        user.setBalance(user.getBalance().subtract(price));
                        userRepository.updateUser(user);
                        balanceDetailRepository.addUserBill(
                                modelPrice.getType().equals(AiPriceTypeEnums.by_count.getCode()) ? modelPrice.getModel() + "，调用1次" : modelPrice.getModel() + "，输入：" + promptTokenCount + "，输出：" + (totalTokenCount - promptTokenCount),
                                user.getUserID(), price, user.getBalance(), user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.CONSUMPTION, "");
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
            }
        }

        return Map.of("promptTokenCount", promptTokenCount, "candidatesTokenCount", candidatesTokenCount, "totalTokenCount", totalTokenCount);
    }

    private Map<String, Long> saveTokenStatisticsChatgpt(User user, String respStr, AiModelPrice modelPrice) {
        Long promptTokenCount = 0L;
        Long candidatesTokenCount = 0L;
        Long totalTokenCount = 0L;
        String model = "";
        try {
            JSONObject jsonObject = JSON.parseObject(respStr);
            JSONObject metadata = jsonObject.getJSONObject("usage");
            promptTokenCount = metadata.getLong("prompt_tokens");
            candidatesTokenCount = metadata.getLong("completion_tokens");
            totalTokenCount = metadata.getLong("total_tokens");
            model = jsonObject.getString("model");
        } catch (Exception e) {}

        if (promptTokenCount == null || promptTokenCount == 0L) {
            return Map.of("promptTokenCount", 0L, "candidatesTokenCount", 0L, "totalTokenCount", 0L);
        }

        if (user == null) {
            return Map.of("promptTokenCount", promptTokenCount, "candidatesTokenCount", candidatesTokenCount, "totalTokenCount", totalTokenCount);
        }
        UserApiKeyTokenStatistics userApiKey = userApiKeyTokenStatisticsRepository.findOneByUserApiKey(user.getUserApiKey());

        for (int i = 0; i < 10; i++) {
            try {
                if (userApiKey == null) {
                    userApiKey = new UserApiKeyTokenStatistics();
                    userApiKey.setApiKey(user.getUserApiKey());
                    userApiKey.setUserID(user.getUserID());
                    userApiKey.setCandidatesTokenCount(0L);
                    userApiKey.setPromptTokenCount(0L);
                    userApiKey.setTotalTokenCount(0L);
                    userApiKey.setCreateTime(new Date());
                    userApiKey.setCallCount(0L);
                    userApiKey.setOutputTokenCount(0L);
                }
                if (userApiKey.getTotalTokenCount() == null) {
                    userApiKey.setTotalTokenCount(0L);
                }
                if (userApiKey.getPromptTokenCount() == null) {
                    userApiKey.setPromptTokenCount(0L);
                }
                if (userApiKey.getCallCount() == null) {
                    userApiKey.setCallCount(0L);
                }
                if (userApiKey.getCandidatesTokenCount() == null) {
                    userApiKey.setCandidatesTokenCount(0L);
                }
                if (userApiKey.getOutputTokenCount() == null) {
                    userApiKey.setOutputTokenCount(userApiKey.getTotalTokenCount() - userApiKey.getPromptTokenCount());
                }
                userApiKey.setOutputTokenCount(userApiKey.getOutputTokenCount() + totalTokenCount - promptTokenCount);
                userApiKey.setPromptTokenCount(userApiKey.getPromptTokenCount() + promptTokenCount);
                userApiKey.setTotalTokenCount(userApiKey.getTotalTokenCount() + totalTokenCount);
                userApiKey.setCandidatesTokenCount(userApiKey.getCandidatesTokenCount() + candidatesTokenCount);
                userApiKey.setCallCount(userApiKey.getCallCount() + 1);
                userApiKeyTokenStatisticsRepository.save(userApiKey);
                break;
            } catch (Exception ex) {
                userApiKey = userApiKeyTokenStatisticsRepository.findOneByUserApiKey(user.getUserApiKey());
            }
        }

        BigDecimal price = calcPrice(user, promptTokenCount, candidatesTokenCount, totalTokenCount, modelPrice.getModel());

        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            RLock lock = redissonClient.getLock(user.getUserID() + "-charge");
            try {
                if (lock.tryLock(30, TimeUnit.SECONDS)) {
                    try {
                        user = userRepository.findOneByUserID(user.getUserID());
                        user.setBalance(user.getBalance().subtract(price));
                        userRepository.updateUser(user);
                        balanceDetailRepository.addUserBill(
                                modelPrice.getType().equals(AiPriceTypeEnums.by_count.getCode()) ? modelPrice.getModel() + "，调用1次" : modelPrice.getModel() + "，输入：" + promptTokenCount + "，输出：" + (totalTokenCount - promptTokenCount),
                                user.getUserID(), price, user.getBalance(), user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.CONSUMPTION, "");
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
            }
        }

        return Map.of("promptTokenCount", promptTokenCount, "candidatesTokenCount", candidatesTokenCount, "totalTokenCount", totalTokenCount);
    }

    private BigDecimal calcPrice(User user, Long promptTokenCount, Long candidatesTokenCount, Long totalTokenCount, String model) {
        if (user == null) {
            return BigDecimal.ZERO;
        }
        AiModelPrice modelPrice = aiModelPriceRepository.findByModel(model);
        if (modelPrice == null || !modelPrice.getStatus().equals("enable")) {
            return BigDecimal.ZERO;
        }
        if (modelPrice.getType().equals(AiPriceTypeEnums.by_count.getCode())) {
            return modelPrice.getCountPrice();
        }
        BigDecimal price = BigDecimal.ZERO;
        price = price.add(BigDecimal.valueOf(promptTokenCount).multiply(modelPrice.getInputTokenPrice()).divide(BigDecimal.valueOf(1000_000), 12, RoundingMode.HALF_UP));
        price = price.add(BigDecimal.valueOf(totalTokenCount - promptTokenCount).multiply(modelPrice.getOutputTokenPrice()).divide(BigDecimal.valueOf(1000_000), 12, RoundingMode.HALF_UP));
        return price;
    }

    private void saveCallInfo(Map<String, Object> params, String respStr, User user, String googleStudioApiKey) {
        // 包含这个才保存
        if (!respStr.contains("promptTokenCount") && !respStr.contains("prompt_tokens")) {
            return;
        }
        UserApiKeyGeminiCallInfo userApiKeyGeminiCallInfo = new UserApiKeyGeminiCallInfo();
        userApiKeyGeminiCallInfo.setGeminiApiKey(googleStudioApiKey);
        if (user != null) {
            userApiKeyGeminiCallInfo.setUserID(user.getUserID());
            userApiKeyGeminiCallInfo.setUserKeyApi(user.getUserApiKey());
        } else {
            userApiKeyGeminiCallInfo.setUserID(Constants.ADMIN_USER_ID);
            userApiKeyGeminiCallInfo.setUserKeyApi("");
        }
        String prompt = JSON.toJSONString(params);
        try {
            if (prompt.length() > 10000) {
                prompt = ((Map) ((List) ((Map) ((List) params.get("contents")).getLast()).get("parts")).getLast()).get("text").toString();
            }
        }catch (Exception e) {}
        if (prompt.length() > 10000) {
            prompt = prompt.substring(0, 10000) + "...";
        }
        userApiKeyGeminiCallInfo.setPrompt(prompt);
        userApiKeyGeminiCallInfo.setResponseTxt(respStr);
        userApiKeyGeminiCallInfo.setCreateTime(new Date());

        userApiKeyGeminiCallInfoRepository.save(userApiKeyGeminiCallInfo);
    }

    private void saveCallInfoChatgpt(Map<String, Object> params, String respStr, User user, String googleStudioApiKey) {
        if (!respStr.contains("promptTokenCount") && !respStr.contains("prompt_tokens")) {
            return;
        }
        UserApiKeyGeminiCallInfo userApiKeyGeminiCallInfo = new UserApiKeyGeminiCallInfo();
        userApiKeyGeminiCallInfo.setGeminiApiKey(googleStudioApiKey);
        if (user != null) {
            userApiKeyGeminiCallInfo.setUserID(user.getUserID());
            userApiKeyGeminiCallInfo.setUserKeyApi(user.getUserApiKey());
        } else {
            userApiKeyGeminiCallInfo.setUserID(Constants.ADMIN_USER_ID);
            userApiKeyGeminiCallInfo.setUserKeyApi("");
        }
        String prompt = JSON.toJSONString(params);
        try {
            if (prompt.length() > 10000) {
                prompt = ((Map)((List) params.get("messages")).getLast()).get("content").toString();
            }
        }catch (Exception e) {}
        if (prompt.length() > 10000) {
            prompt = prompt.substring(0, 10000) + "...";
        }
        userApiKeyGeminiCallInfo.setPrompt(prompt);
        userApiKeyGeminiCallInfo.setResponseTxt(respStr);
        userApiKeyGeminiCallInfo.setCreateTime(new Date());

        userApiKeyGeminiCallInfoRepository.save(userApiKeyGeminiCallInfo);
    }

    // 邮箱检测
    @NoLogin
    @PostMapping("/v1beta/uploadTxt/**")
    public Result<UploadFileVO> uploadFileTxt(@RequestHeader("token") String token, @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (!token.equals("AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE")) {
            ApiKey byApiKey = apiKeyRepository.findByApiKey(token);
            if (byApiKey == null || !byApiKey.getUserID().equals(Constants.ADMIN_USER_ID)) {
                return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID, "token is invalid");
            }
        }
        // 1. 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null) {
            log.warn("上传失败，文件类型不被允许: {}", contentType);
            return ResponseResult.failure(ResultCode.FILE_TYPE_NOT_ALLOWED, "仅支持上传图片类型文件");
        }

        // 2. 校验文件扩展名（进一步确认）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches("(?i)^.+\\.(txt|csv)$")) {
            log.warn("上传失败，文件扩展名不合法: {}", originalFilename);
            return ResponseResult.failure(ResultCode.FILE_TYPE_NOT_ALLOWED, "仅支持上传文件（.txt/.csv）");
        }

        // 获取路径
        String contextPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        contextPath = contextPath.replaceAll("\\.\\.", "");

        UploadFileVO uploadFileVO = new UploadFileVO();
        uploadFileVO.setName(originalFilename);
        uploadFileVO.setType(contentType);

        Path resPath = FileUtils.resPath;
        uploadFileVO.setType(contextPath.substring(contextPath.indexOf("upload") + "uploadTxt/".length()));
        Path folder = Paths.get(uploadFileVO.getType());
        try {
            Files.createDirectories(resPath.resolve(folder).normalize());
        } catch (IOException e) {
            log.error("创建upload文件夹失败", e);
            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
        }

        String ext = null;
        String oldFileName = null;
        try {
            ext = FileUtil.getSuffix(originalFilename);
            oldFileName = FileUtil.getPrefix(originalFilename);
        } catch (Exception e2) {
            log.error("根据文件名获取文件后缀失败", e2);
        }

        String fileName = org.springframework.util.StringUtils.cleanPath(oldFileName + "-" + System.currentTimeMillis() + "." + ext);
        if (fileName.contains("..")) {
            log.error("文件名不能包含.. {}", fileName);
            return ResponseResult.failure(ResultCode.FILENAME_BAD_REQUEST);
        }

        Path filePath = folder.resolve(fileName).normalize();
        Path storePath = resPath.resolve(filePath).normalize();
        uploadFileVO.setFilepath(filePath.toString());

        try {
            Files.copy(file.getInputStream(), storePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件存储失败", e);
            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
        }

        return ResponseResult.success(uploadFileVO);
    }

    @NoLogin
    @PostMapping({"/v1beta/saveEmailCheckActive"})
    public Result<GroupTask> saveEmailCheckActive(@RequestHeader("token") String token, @RequestBody(required = false) EmailCheckActiveReqDto reqDTO) {
        if (!token.equals("AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE")) {
            ApiKey byApiKey = apiKeyRepository.findByApiKey(token);
            if (byApiKey == null || !byApiKey.getUserID().equals(Constants.ADMIN_USER_ID)) {
                return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID, "token is invalid");
            }
        }
        GroupTask save = emailCheckActiveService.save(reqDTO, Constants.ADMIN_USER_ID);
        save.setIds(null);
        return ResponseResult.success(save);
    }

    @NoLogin
    @PostMapping({"/v1beta/queryEmailCheckActive"})
    public Result<GroupTask> queryEmailCheckActive(@RequestHeader("token") String token, @RequestBody(required = false) IdsListDTO reqDto) {
        if (!token.equals("AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE")) {
            ApiKey byApiKey = apiKeyRepository.findByApiKey(token);
            if (byApiKey == null || !byApiKey.getUserID().equals(Constants.ADMIN_USER_ID)) {
                return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID, "token is invalid");
            }
        }
        GroupTask save = groupTaskRepository.findById(reqDto.getId()).orElse(null);
        if (save != null) {
            save.setIds(null);
        }
        return ResponseResult.success(save);
    }

    @NoLogin
    @PostMapping({"/v1beta/v1/chat/completions"})
    public ResponseEntity<Map> openaiChat(@RequestHeader(value = "Authorization", required = false) String token, @RequestParam(value = "key", required = false) String apiKey, @RequestBody(required = false) Map<String, Object> params) {
        User user = null;
        if (StringUtils.isEmpty(apiKey)) {
            apiKey = token.replace("Bearer ", "");
        }
        if (!apiKey.equals("AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE")) {
            user = userService.findUserByUserApiKey(apiKey);
            if (user == null) {
                ApiKey byApiKey = apiKeyRepository.findByApiKey(apiKey);
                if (byApiKey == null || !byApiKey.getUserID().equals(Constants.ADMIN_USER_ID)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new HashMap(Map.of("code", ResultCode.PARAMS_IS_INVALID, "detail", "key is invalid")));
                }
            } else {
                if (user.getStatus().equals(UserStatusEnum.DISABLED.getCode())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new HashMap(Map.of("code", ResultCode.ACCOUNT_BANNED, "detail", ResultCode.ACCOUNT_BANNED.getMessage())));
                }
            }
        }
        String model = params.getOrDefault("model", "auto").toString();
        AiModelPrice modelPrice = aiModelPriceRepository.findByModel(model);
        if (user != null && (modelPrice == null || !modelPrice.getStatus().equals("enable"))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashMap(Map.of("code", ResultCode.NO_SUPPORT_MODEL, "error", ResultCode.NO_SUPPORT_MODEL.getMessage() + ":" + model)));
        }
        if (user != null && user.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashMap(Map.of("code", ResultCode.USER_BALANCE_INSUFFICIENT, "error", ResultCode.USER_BALANCE_INSUFFICIENT.getMessage())));
        }
        if (taskType.equals("googleai")) {
            if (googleModels.contains(model)) {
                GoogleStudioApiKey googleStudioApiKey = null;
                String responseStr = "";
                for (int i = 0; i < 10; i++) {
                    int waitInterval = 10;
                    try {
                        List<GoogleStudioApiKey> googleStudioApiKeyList = googleStudioApiKeyRepository.findByCanUsed(100, Constants.ADMIN_USER_ID, null, AiTypeEnums.GoogleStudio)
                                .stream().filter(e -> e.getUseByDay() < e.getLimitByDay() && e.getUseByMinute() < e.getLimitByMinute()).collect(Collectors.toList());
                        if (googleStudioApiKeyList.isEmpty()) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", ResultCode.INTERNAL_SERVER_ERROR.getCode(), "error", "没有可用的余额"));
                        }
                        // 随机取一个
                        Collections.shuffle(googleStudioApiKeyList);
                        googleStudioApiKey = googleStudioApiKeyList.getFirst();

                        if (i > 0) {
                            socks5Service.releaseSocks5(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null);
                            googleStudioApiKey.setSocks5Id("");
                        }
                        Socks5 socks5 = null;
                        if (StringUtils.isEmpty(googleStudioApiKey.getSocks5Id())) {
                            socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                            if (socks5 == null) {
                                throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                            }
                            googleStudioApiKey.setSocks5Id(socks5.get_id());
                            googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
                        } else {
                            socks5 = socks5Repository.findSocks5ById(googleStudioApiKey.getSocks5Id());
                            if (socks5 == null) {
                                socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                                if (socks5 == null) {
                                    throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                                }
                                googleStudioApiKey.setSocks5Id(socks5.get_id());
                                googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
                            }
                        }
                        if (socks5 == null) {
                            throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                        }

                        // 更新次数
                        for (int j = 0; j < 10; j++) {
                            try {
                                googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                                googleStudioApiKey.setUseByMinute(googleStudioApiKey.getUseByMinute() + 1);
                                googleStudioApiKey.setUseByDay(googleStudioApiKey.getUseByDay() + 1);
                                googleStudioApiKeyRepository.update(googleStudioApiKey);
                                break;
                            } catch (Exception e) {
                                log.info("ImageRecognition ERROR update 1: {}", e.getMessage());
                            }
                        }

                        String type = AiTypeEnums.GoogleStudio.getCode();
                        if (StringUtils.isNotEmpty(googleStudioApiKey.getType())) {
                            type = googleStudioApiKey.getType();
                        }

                        OpenAIRequest openAIRequest = JSON.parseObject(JSON.toJSONString(params), OpenAIRequest.class);
                        GeminiRequest geminiRequest = GeminiRequest.convert(openAIRequest);

                        AiImageRecognition imageRecognition = applicationContext.getBean(type, AiImageRecognition.class);
                        String contentOptimize = imageRecognition.proxyMode(socks5, googleStudioApiKey.getApiKey(), JSON.toJSONString(Map.of("contents", geminiRequest.prompt)), model);
                        if (StringUtils.isNotEmpty(contentOptimize)) {
                            // 成功使用识别
                            for (int j = 0; j < 10; j++) {
                                try {
                                    googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                                    googleStudioApiKey.setUsedSuccess(googleStudioApiKey.getUsedSuccess() + 1);
                                    googleStudioApiKey.setUsedSuccessByDay(googleStudioApiKey.getUsedSuccessByDay() + 1);
                                    Date lastSuccessTime = googleStudioApiKey.getLastSuccessTime();
                                    if (lastSuccessTime != null) {
                                        googleStudioApiKey.setTwoSuccessInterval((int) ((new Date().getTime() - lastSuccessTime.getTime()) / 1000));
                                    }
                                    googleStudioApiKey.setLastSuccessTime(new Date());
                                    googleStudioApiKey.setConsecutiveFailureCount(0);
                                    googleStudioApiKeyRepository.update(googleStudioApiKey);
                                    break;
                                } catch (Exception e) {
                                    log.info("ImageRecognition ERROR update 2: {}", e.getMessage());
                                }
                            }
                            saveCallInfo(params, contentOptimize, user, googleStudioApiKey.getApiKey());
                            Map<String, Long> map = saveTokenStatistics(user, contentOptimize, modelPrice);
                            Long promptTokenCount = map.get("promptTokenCount");
                            Long candidatesTokenCount = map.get("candidatesTokenCount");
                            Long totalTokenCount = map.get("totalTokenCount");
                            //{"id":"chatcmpl-pX7JjASTZL26xoUiQeWoGYjgDYYoJ","object":"chat.completion","created":1760327496,"model":"auto","choices":[{"index":0,"message":{"role":"assistant","content":"��ã����� **GPT-5**���� **OpenAI** ����������һ����������ģ�͡�����˵������һ�����ܶԻ����֣���������������Ȼ�����ı���������ɸ����������磺  \n\n- ? **��������ѯ**�����㽻����ͷ�Է籩�����͸��Ӹ���  \n- ?? **д����ĸ�**��д���¡����桢�ʼ������ġ�С˵��  \n- ? **��������**�����ɡ��Ż�����ʹ��루֧�ֶ������ԣ�  \n- ? **ѧϰ�빤������**���ܽ����ϡ��������ݡ�����PPT������ʼ�  \n- ? **������֧��**�����������ġ�Ӣ�ĵȶ������Լ������л�  \n- ? **��������**�����㹹˼���¡�����Ӿ����ݡ�д��ʻ�ű�  \n\n�Ҳ��ᱣ�������˽��Ϣ���������ر����ҡ���ס��ĳ���£��������ϲ�û�����Ŀ����  \n\n�����ҽ��ܵø�����һ�㣨����ģ�ͽṹ��������Χ�������Ǹ�����һ�㣨�������������ҽ��ܣ���"},"logprobs":null,"finish_reason":"stop"}],"usage":{"prompt_tokens":16,"completion_tokens":369,"total_tokens":385}}
                            JSONObject objRes = JSONObject.parseObject(contentOptimize);
                            if (objRes.containsKey("error")) {
                                throw new CommonException(ResultCode.GOOGLE_AI_ERROR, objRes.getJSONObject("error").toString());
                            }
                            if (promptTokenCount == null || promptTokenCount == 0L) {
                                throw new CommonException(ResultCode.GOOGLE_AI_ERROR, contentOptimize);
                            }
                            // "usage":{"prompt_tokens":16,"completion_tokens":369,"total_tokens":385}
                            String content = objRes.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                            return ResponseEntity.status(HttpStatus.OK).body(Map.of("id", UUIDUtils.get32UUId(), "object", "chat.completion",
                                    "created", System.currentTimeMillis() / 1000, "model", model, "choices", List.of(Map.of("index", 0,
                                            "message", Map.of("role", "assistant", "content", content), "finish_reason", "stop")), "usage", Map.of("prompt_tokens", promptTokenCount, "completion_tokens", totalTokenCount - promptTokenCount, "total_tokens", totalTokenCount)));
                        }
                    } catch (Exception e) {
                        responseStr = e.getMessage();
                        if (googleStudioApiKey != null) {
                            // 释放IP
                            socks5Service.releaseSocks5(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null);
                            googleStudioApiKey.setSocks5Id("");

                            // if (e instanceof CommonException && (e.getMessage().contains("403") || e.getMessage().equals("API key not valid"))) {
                            if (e instanceof CommonException && (e.getMessage().contains("403") || e.getMessage().contains("API key not valid") || e.getMessage().contains("User location is not supported for the API use."))) {
                                googleStudioApiKeyRepository.updateDisable(googleStudioApiKey.get_id());
                            } else if (e instanceof CommonException && (e.getMessage().contains("429"))) {
                                waitInterval = 60 * 60; // 10分钟不可用
                            } else {
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("code", ResultCode.INTERNAL_SERVER_ERROR.getCode(), "error", e.getMessage()));
                            }
                        }
                    } finally {
                        if (googleStudioApiKey != null) {
                            String socks5Id = googleStudioApiKey.getSocks5Id();
                            googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                            if (googleStudioApiKey != null) {
                                if (waitInterval >= 60 * 60) {
                                    int tooManyWaitSecond = Integer.parseInt(paramsService.getParams("account.tooManyWaitSecond", null, null).toString());

                                    if (googleStudioApiKey.getConsecutiveFailureCount() == null) {
                                        googleStudioApiKey.setConsecutiveFailureCount(1);
                                    } else {
                                        googleStudioApiKey.setConsecutiveFailureCount(googleStudioApiKey.getConsecutiveFailureCount() + 1);
                                    }
                                    waitInterval = tooManyWaitSecond * googleStudioApiKey.getConsecutiveFailureCount();
                                }
                                // 修改google下次使用时间
                                Calendar instance = Calendar.getInstance();
                                instance.add(Calendar.SECOND, waitInterval);
                                googleStudioApiKey.setNextUseTime(instance.getTime());
                                googleStudioApiKey.setSocks5Id(socks5Id);

                                googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), googleStudioApiKey.getNextUseTime(), googleStudioApiKey.getConsecutiveFailureCount());
                            }
                        }
                    }
                }
                try {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", JSON.parseObject(responseStr, Map.class)));
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", responseStr));
                }
            }

            if (chatgptModels.contains(model)) {
                GoogleStudioApiKey googleStudioApiKey = null;
                String responseStr = "";
                for (int i = 0; i < 10; i++) {
                    int waitInterval = 10;
                    try {
                        List<GoogleStudioApiKey> googleStudioApiKeyList = googleStudioApiKeyRepository.findByCanUsed(100, Constants.ADMIN_USER_ID, null, AiTypeEnums.Chatgpt)
                                .stream().filter(e -> e.getUseByDay() < e.getLimitByDay() && e.getUseByMinute() < e.getLimitByMinute()).collect(Collectors.toList());
                        if (googleStudioApiKeyList.isEmpty()) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", ResultCode.INTERNAL_SERVER_ERROR.getCode(), "error", "没有可用的余额"));
                        }
                        // 随机取一个
                        Collections.shuffle(googleStudioApiKeyList);
                        googleStudioApiKey = googleStudioApiKeyList.getFirst();

                        if (i > 0) {
                            socks5Service.releaseSocks5(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null);
                            googleStudioApiKey.setSocks5Id("");
                        }
                        Socks5 socks5 = null;
                        if (StringUtils.isEmpty(googleStudioApiKey.getSocks5Id())) {
                            socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                            if (socks5 == null) {
                                throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                            }
                            googleStudioApiKey.setSocks5Id(socks5.get_id());
                            googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
                        } else {
                            socks5 = socks5Repository.findSocks5ById(googleStudioApiKey.getSocks5Id());
                            if (socks5 == null) {
                                socks5 = proxyAccountService.getProxy(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                                if (socks5 == null) {
                                    throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                                }
                                googleStudioApiKey.setSocks5Id(socks5.get_id());
                                googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null, null);
                            }
                        }
                        if (socks5 == null) {
                            throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
                        }

                        // 更新次数
                        for (int j = 0; j < 10; j++) {
                            try {
                                googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                                googleStudioApiKey.setUseByMinute(googleStudioApiKey.getUseByMinute() + 1);
                                googleStudioApiKey.setUseByDay(googleStudioApiKey.getUseByDay() + 1);
                                googleStudioApiKeyRepository.update(googleStudioApiKey);
                                break;
                            } catch (Exception e) {
                                log.info("ImageRecognition ERROR update 1: {}", e.getMessage());
                            }
                        }

                        String type = AiTypeEnums.GoogleStudio.getCode();
                        if (StringUtils.isNotEmpty(googleStudioApiKey.getType())) {
                            type = googleStudioApiKey.getType();
                        }

                        AiImageRecognition imageRecognition = applicationContext.getBean(type, AiImageRecognition.class);
                        String contentOptimize = imageRecognition.proxyMode(socks5, googleStudioApiKey.getApiKey(), JSON.toJSONString(params), model);
                        if (StringUtils.isNotEmpty(contentOptimize)) {
                            // 成功使用识别
                            for (int j = 0; j < 10; j++) {
                                try {
                                    googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                                    googleStudioApiKey.setUsedSuccess(googleStudioApiKey.getUsedSuccess() + 1);
                                    googleStudioApiKey.setUsedSuccessByDay(googleStudioApiKey.getUsedSuccessByDay() + 1);
                                    Date lastSuccessTime = googleStudioApiKey.getLastSuccessTime();
                                    if (lastSuccessTime != null) {
                                        googleStudioApiKey.setTwoSuccessInterval((int) ((new Date().getTime() - lastSuccessTime.getTime()) / 1000));
                                    }
                                    googleStudioApiKey.setLastSuccessTime(new Date());
                                    googleStudioApiKey.setConsecutiveFailureCount(0);
                                    googleStudioApiKeyRepository.update(googleStudioApiKey);
                                    break;
                                } catch (Exception e) {
                                    log.info("ImageRecognition ERROR update 2: {}", e.getMessage());
                                }
                            }
                            Map<String, String> map2 = JSON.parseObject(contentOptimize, Map.class);
                            if (map2.get("code").equals("200")) {
                                saveCallInfoChatgpt(params, map2.get("respStr"), user, googleStudioApiKey.getApiKey());
                                Map<String, Long> map = saveTokenStatisticsChatgpt(user, map2.get("respStr"), modelPrice);
                                return ResponseEntity.status(Integer.valueOf(map2.get("code"))).body(JSON.parseObject(map2.get("respStr"), Map.class));
                            } else {
                                if (map2.get("code").equals("401")) {
                                    googleStudioApiKeyRepository.updateDisable(googleStudioApiKey.get_id());
                                }
                            }
                        }
                    } catch (Exception e) {
                        responseStr = e.getMessage();
                        if (googleStudioApiKey != null) {
                            // 释放IP
                            socks5Service.releaseSocks5(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), null);
                            googleStudioApiKey.setSocks5Id("");

                            // if (e instanceof CommonException && (e.getMessage().contains("403") || e.getMessage().equals("API key not valid"))) {
                            if (e instanceof CommonException && (e.getMessage().contains("403") || e.getMessage().contains("API key not valid") || e.getMessage().contains("User location is not supported for the API use."))) {
                                waitInterval = 60 * 60; // 10分钟不可用
                            } else if (e instanceof CommonException && (e.getMessage().contains("429"))) {
                                waitInterval = 60 * 60; // 10分钟不可用
                            } else {
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("code", ResultCode.INTERNAL_SERVER_ERROR.getCode(), "error", e.getMessage()));
                            }
                        }
                    } finally {
                        if (googleStudioApiKey != null) {
                            String socks5Id = googleStudioApiKey.getSocks5Id();
                            googleStudioApiKey = googleStudioApiKeyRepository.findOneByIdAndUserID(googleStudioApiKey.get_id(), googleStudioApiKey.getUserID());
                            if (googleStudioApiKey != null) {
                                if (waitInterval >= 60 * 60) {
                                    int tooManyWaitSecond = Integer.parseInt(paramsService.getParams("account.tooManyWaitSecond", null, null).toString());

                                    if (googleStudioApiKey.getConsecutiveFailureCount() == null) {
                                        googleStudioApiKey.setConsecutiveFailureCount(1);
                                    } else {
                                        googleStudioApiKey.setConsecutiveFailureCount(googleStudioApiKey.getConsecutiveFailureCount() + 1);
                                    }
                                    waitInterval = tooManyWaitSecond * googleStudioApiKey.getConsecutiveFailureCount();
                                }
                                // 修改google下次使用时间
                                Calendar instance = Calendar.getInstance();
                                instance.add(Calendar.SECOND, waitInterval);
                                googleStudioApiKey.setNextUseTime(instance.getTime());
                                googleStudioApiKey.setSocks5Id(socks5Id);

                                googleStudioApiKeyRepository.updateSocks5AndNextUseTime(googleStudioApiKey.get_id(), googleStudioApiKey.getSocks5Id(), googleStudioApiKey.getNextUseTime(), googleStudioApiKey.getConsecutiveFailureCount());
                            }
                        }
                    }
                }
                try {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", JSON.parseObject(responseStr, Map.class)));
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", responseStr));
                }
            }
        } else {
            String jsonBody = JSON.toJSONString(params);
            String md5 = MD5Util.MD5(jsonBody);
            Object o = redisUtil.get("REQ_LIMIT:" + md5);
            if (o != null) {
                if (Integer.valueOf(o.toString()) >= 15) {
                    throw new CommonException(ResultCode.TOO_MANY_REQUEST, "请求太频繁");
                }
                redisUtil.set("REQ_LIMIT:" + md5, Integer.valueOf(o.toString()) + 1 + "", 60);
            } else {
                redisUtil.set("REQ_LIMIT:" + md5, "1", 60);
            }

            for (int i = 0; i < 10; i++) {
                GoogleAiServer googleAiServer = googleAiServerService.selectOneServer(true);
                if (googleAiServer == null) {
                    throw new CommonException(ResultCode.NO_ONLINE_ACCOUNT, "没有可用服务器");
                }
                OkHttpClient defaultClient = OkHttpClientFactory.getDefaultClient();
                // JSON 请求体
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");


                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, mediaType);

                Request request = null;
                Request.Builder builder = new Request.Builder().url(googleAiServer.getUrl() + "/api/open/v1beta/v1/chat/completions?key=AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE");
                request = builder.post(body).build();

                try (Response response = defaultClient.newCall(request).execute()) {
                    if (response.body() != null) {
                        String respStr = response.body().string();
                        if (!respStr.contains("没有可用的余额")) {
                            if (user != null) {
                                saveCallInfoChatgpt(params, respStr, user, "");
                                saveTokenStatisticsChatgpt(user, respStr, modelPrice);
                            }

                            return ResponseEntity.status(response.code()).body(JSONObject.parseObject(respStr, Map.class));
                        }
                    } else {
                        throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, "请求超时，请重试");
                    }
                } catch (IOException e) {
                    log.error("Request failed error: {}", e.getMessage());
                    throw new CommonException(ResultCode.INTERNAL_SERVER_ERROR, e.getMessage());
                }
            }
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("code", ResultCode.INTERNAL_SERVER_ERROR.getCode(), "error", ResultCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

}
