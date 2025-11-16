package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.UserApiKeyTokenStatistics;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.service.UserApiKeyTokenStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping({"/api/userApiKeyTokenStatistics"})
public class UserApiKeyTokenStatisticsController {

    @Autowired
    private UserApiKeyTokenStatisticsService userApiKeyTokenStatisticsService;

    // list
    @PostMapping("/list/{pageSize}/{page}")
    public Result<PageResult<UserApiKeyTokenStatistics>> list(@PathVariable int pageSize, @PathVariable int page, @RequestBody(required = false) Params params) {
        String userID = Session.currentSession().getUserID();
        if (params.getFilters() == null) {
            params.setFilters(new HashMap<>());
        }
        if (!userID.equals(Constants.ADMIN_USER_ID)) {
            params.getFilters().put("userID", userID);
        }
        PageResult<UserApiKeyTokenStatistics> orders = userApiKeyTokenStatisticsService.findByPagination(params, pageSize, page, Session.currentSession().getUserID());
        return ResponseResult.success(orders);
    }

}
