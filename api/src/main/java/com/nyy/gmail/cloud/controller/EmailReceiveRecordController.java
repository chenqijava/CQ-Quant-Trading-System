package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.AccountPlatform;
import com.nyy.gmail.cloud.entity.mongo.EmailReceiveRecord;
import com.nyy.gmail.cloud.model.dto.AccountPlatformReqDto;
import com.nyy.gmail.cloud.repository.mongo.EmailReceiveRecordRepository;
import com.nyy.gmail.cloud.service.AccountPlatformService;
import com.nyy.gmail.cloud.utils.ReflectionUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RestController
@RequiredPermission(MenuType.receive)
@RequestMapping({"/api/receive", "/api/consumer/receive"})
public class EmailReceiveRecordController {

    @Autowired
    private EmailReceiveRecordRepository emailReceiveRecordRepository;

    @PostMapping("{pageSize}/{page}")
    public Result<PageResult<EmailReceiveRecord>> list(@PathVariable("pageSize") int pageSize, @PathVariable("page") int page,
                                                       @RequestBody(required = false) AccountPlatformReqDto data) {
        String userID = Session.currentSession().getUserID();
        data.getFilters().put("userID", userID);
        PageResult<EmailReceiveRecord> accountByPagination = emailReceiveRecordRepository.findByPagination(data, pageSize, page);
        return ResponseResult.success(accountByPagination);
    }

}
