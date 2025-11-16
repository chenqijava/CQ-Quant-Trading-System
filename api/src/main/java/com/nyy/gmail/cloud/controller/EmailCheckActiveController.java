package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.GoogleAiServer;
import com.nyy.gmail.cloud.model.dto.EmailCheckActiveReqDto;
import com.nyy.gmail.cloud.service.EmailCheckActiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping({"/api/emailCheckActive"})
public class EmailCheckActiveController {

    @Autowired
    private EmailCheckActiveService emailCheckActiveService;

    // 1. 保存任务
    @PostMapping({"/save"})
    public Result save(@RequestBody(required = false) EmailCheckActiveReqDto reqDTO) {
        emailCheckActiveService.save(reqDTO, Session.currentSession().userID);
        return ResponseResult.success();
    }

    // 2. 任务列表 -> SubTaskController


    // 3. 取消订单 -> SubTaskController


    // 4. 订单报表

    // 5. 下载邮箱
}
