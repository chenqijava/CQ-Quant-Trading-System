package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.AiStatistics;
import com.nyy.gmail.cloud.service.AiStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/aiStatistics"})
public class AiStatisticsController {

    @Autowired
    private AiStatisticsService aiStatisticsService;

    @PostMapping("{pageSize}/{page}")
    public Result<List<AiStatistics>> list(@PathVariable("pageSize") int pageSize,
                                           @PathVariable("page") int page) {
        String userID = Session.currentSession().getUserID();
        List<AiStatistics> aiStatisticsList = aiStatisticsService.findList(userID, pageSize, page);//按id倒排序，未做到populate
        return ResponseResult.success(aiStatisticsList);
    }
}
