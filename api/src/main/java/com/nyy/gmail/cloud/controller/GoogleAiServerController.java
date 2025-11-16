package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.GoogleAiServer;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.service.GoogleAiServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.Map;

@Slf4j
@RestController
@RequiredPermission(MenuType.aiServer)
@RequestMapping({"/api/aiServer", "/api/consumer/aiServer"})
public class GoogleAiServerController implements Serializable {

    @Autowired
    private GoogleAiServerService googleAiServerService;

    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;

    // 新增
    @PostMapping({"/save"})
    public Result save(@RequestBody(required = false) GoogleAiServer reqDTO) {
        googleAiServerService.save(reqDTO, Session.currentSession().userID);
        return ResponseResult.success();
    }

    // 删除
    @PostMapping({"/delete"})
    public Result delete(@RequestBody(required = false) IdsListDTO ids) {
        googleAiServerService.delete(ids, Session.currentSession().userID);
        return ResponseResult.success();
    }

    // 列表
    @PostMapping({"/{pageSize}/{pageNum}"})
    public Result<PageResult<GoogleAiServer>> list(@PathVariable Integer pageSize, @PathVariable Integer pageNum, @RequestBody(required = false) Map<String, Object> reqDTO) {
        PageResult<GoogleAiServer> query = mongoPaginationHelper.query(
                MongoPaginationBuilder.builder(GoogleAiServer.class).
                        filters(reqDTO)
                        .sorter(null)
                        .pageSize(pageSize).page(pageNum).build());


//        googleAiServerService.uploadSever();
        return ResponseResult.success(query);
    }

    @PostMapping({"/uploadServer"})
    public Result uploadServer() {
        googleAiServerService.uploadSever();
        return ResponseResult.success();
    }
}
