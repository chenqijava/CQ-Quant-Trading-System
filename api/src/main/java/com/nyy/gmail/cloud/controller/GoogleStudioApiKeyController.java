package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.GoogleStudioApiKey;
import com.nyy.gmail.cloud.enums.AiTypeEnums;
import com.nyy.gmail.cloud.model.dto.AccountPlatformReqDto;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.service.GoogleStudioApiKeyService;
import com.nyy.gmail.cloud.utils.ChatgptAiUtils;
import com.nyy.gmail.cloud.utils.ReflectionUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.util.UriEncoder;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredPermission({MenuType.googleStudio, MenuType.chatgpt})
@RequestMapping({"/api/googleStudio"})
public class GoogleStudioApiKeyController {
    // 列表

    // 导入

    // 导出

    // 新增

    // 删除
    @Value("${gateway.chatgptBaseUrl}")
    private String chatgptBaseUrl;

    @PostConstruct
    public void init() {
        ChatgptAiUtils.BASE_URL = chatgptBaseUrl;
    }


    @Autowired
    private GoogleStudioApiKeyService googleStudioApiKeyService;

    @PostMapping("/save")
    public Result save(@RequestBody GoogleStudioApiKey googleStudioApiKey) {
        String userID = Session.currentSession().getUserID();
        googleStudioApiKeyService.save(googleStudioApiKey, userID);
        return ResponseResult.success();
    }

    @PostMapping("/delete")
    public Result delete(@RequestBody List<String> ids) {
        String userID = Session.currentSession().getUserID();
        googleStudioApiKeyService.delete(ids, userID);
        return ResponseResult.success(null);
    }

    @PostMapping("{pageSize}/{page}")
    public Result<PageResult<GoogleStudioApiKey>> list(@PathVariable("pageSize") int pageSize, @PathVariable("page") int page,
                                                    @RequestBody(required = false) AccountPlatformReqDto data) {
        String userID = Session.currentSession().getUserID();
        data.getFilters().put("userID", userID);
        PageResult<GoogleStudioApiKey> accountByPagination = googleStudioApiKeyService.list(data, userID,  pageSize, page);
        return ResponseResult.success(accountByPagination);
    }

    @PostMapping("export")
    public void export(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<String> ids = request.getParameterMap().values().stream().map(e -> e[0]).toList();
        String userID = Session.currentSession().getUserID();
        List<GoogleStudioApiKey> accountPlatforms = googleStudioApiKeyService.findAll(userID);
        accountPlatforms = accountPlatforms.stream().filter(e -> ids.isEmpty() || ids.contains(e.get_id())).toList();
        List<String> lines = accountPlatforms.stream().map(e -> e.getEmail() + "----" + e.getApiKey()
                + "----" + (StringUtils.isEmpty(e.getType()) ? AiTypeEnums.GoogleStudio.getCode() : e.getType())).toList();

        response.setHeader("Cache-Control", "public");

        ServletOutputStream outputStream = response.getOutputStream();
        byte[] buff = String.join("\n", lines).getBytes();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength(buff.length);
        response.setHeader("Content-Disposition", "attachment;filename=" + UriEncoder.encode("AIKEY-"+lines.size()+".txt"));
        outputStream.write(buff, 0, buff.length);
        outputStream.flush();
    }

    @PostMapping("import/{type}")
    public Result importPlatform(@RequestParam("file") MultipartFile file, @PathVariable("type") String type) throws IOException {
        String data = new String(file.getBytes(), StandardCharsets.UTF_8);
        data = data.replace("\r", "");
        int batchSize = 500;
        List<GoogleStudioApiKey> entities = new ArrayList<>(batchSize);
        int repeat = 0;
        String[] lines = data.split("\n");
        int total = lines.length;
        for (String line : lines) {
            String[] sv = line.split("----", -1);
            Map<String, String> map = new HashMap<>();
            map.put("email", sv[0].trim());
            map.put("apiKey", sv[1].trim());

            String userID = Session.currentSession().getUserID();
            map.put("userID", userID);

            GoogleStudioApiKey platform = new GoogleStudioApiKey();
            ReflectionUtil.setProperties(map, platform);
            platform.setVersion(0L);
            platform.setCreateTime(new Date());
            platform.setType(type);

            if (entities.stream().noneMatch(e -> e.getApiKey().equals(platform.getApiKey()))) {
                entities.add(platform);
            }
            if (entities.size() >= batchSize) {
                int success = googleStudioApiKeyService.saveBatch(entities);
                repeat += entities.size() - success;
                entities = new ArrayList<>();
            }
        }
        if (!entities.isEmpty()) {
            int success = googleStudioApiKeyService.saveBatch(entities);
            repeat += entities.size() - success;
        }

        return ResponseResult.success();
    }

    @PostMapping("test")
    public Result test(@RequestBody(required = false)IdsListDTO data) {
        try {
            googleStudioApiKeyService.test(data, Session.currentSession().userID);
        } catch (Exception e) {
            return ResponseResult.failure(400, "检查报错：" + e.getMessage());
        }
        return ResponseResult.success();
    }

    @PostMapping("canUseCount")
    public Result<Map<String, Integer>> canUseCount() {
        return  ResponseResult.success(googleStudioApiKeyService.canUseCount(Session.currentSession().userID));
    }
}
