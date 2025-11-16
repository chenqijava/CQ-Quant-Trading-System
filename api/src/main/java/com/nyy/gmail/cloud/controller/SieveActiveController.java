package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.ProxyAccount;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.model.dto.SieveActiveReqDto;
import com.nyy.gmail.cloud.model.dto.SieveActiveStopTaskReqDto;
import com.nyy.gmail.cloud.service.SieveActiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/sieveActive"})
public class SieveActiveController {
    @Autowired
    private SieveActiveService sieveActiveService;
    @Autowired
    private MongoPaginationHelper paginationHelper;
    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;

    @PostMapping({"/save"})
    public Result<Void> save(@RequestBody @Validated SieveActiveReqDto reqDTO) throws Exception {
        sieveActiveService.save(reqDTO, Session.currentSession().userID);
        return ResponseResult.success();
    }

    @PostMapping("stop")
    public Result<Void> stop(@RequestBody @Validated SieveActiveStopTaskReqDto reqDTO) throws Exception {
        sieveActiveService.stop(reqDTO.getIds(), reqDTO.getForceStop(), Session.currentSession().userID);
        return ResponseResult.success();
    }

    @PostMapping("listSubTask/{pageSize}/{page}")
    public Result<PageResult<SubTask>> stop(@PathVariable int pageSize, @PathVariable int page, @RequestBody(required = false) Params params) throws Exception {
        PageResult<SubTask> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(SubTask.class)
                .filters(params.getFilters())
                .sorter(params.getSorter())
                .pageSize(pageSize)
                .page(page)
                .build());
        return ResponseResult.success(pageResult);
    }
}
