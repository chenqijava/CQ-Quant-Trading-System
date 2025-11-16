package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.service.ContentAIGenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredPermission(MenuType.ContentAIGen)
@RequestMapping({"/api/contentAIGen"})
public class ContentAIGenController {

    @Autowired
    private ContentAIGenService contentAIGenService;


    @PostMapping({"/save"})
    public Result save(@RequestBody(required = false) ContentAIGenReqDto reqDTO) {
        contentAIGenService.save(reqDTO, Session.currentSession().userID);
        return ResponseResult.success();
    }

    // 2. 列表
    @PostMapping({"/{pageSize}/{pageNum}"})
    public Result<PageResult<GroupTask>> list(@PathVariable Integer pageSize, @PathVariable Integer pageNum,
                                              @RequestBody(required = false) Params params) {
        if (params.getFilters() == null) {
            params.setFilters(new HashMap<>());
        }
        params.getFilters().put("userID", Session.currentSession().userID);
        params.getFilters().put("type", TaskTypesEnums.EmailContentAiGen.getCode());

        PageResult<GroupTask> pageResult = contentAIGenService.findByPagination(params, pageSize, pageNum);
        pageResult.getData().forEach(e -> {
            e.setIds(null);
        });
        return ResponseResult.success(pageResult);
    }

    // 3. 删除
    @PostMapping({"/delete"})
    public Result delete(@RequestBody(required = false) IdsListDTO data) {
        contentAIGenService.delete(data);
        return ResponseResult.success();
    }

    // 5. 结束任务
    @PostMapping({"/stop"})
    public Result stop(@RequestBody(required = false)IdsListDTO data) {
        contentAIGenService.stop(data);
        return ResponseResult.success();
    }

    @PostMapping({"/detail/{groupTaskId}/{pageSize}/{pageNum}"})
    public Result<PageResult<ContentAiGenDetail>> detail(@PathVariable String groupTaskId, @PathVariable Integer pageSize, @PathVariable Integer pageNum, @RequestBody(required = false) Map sorts) {
        PageResult<ContentAiGenDetail> pageResult = contentAIGenService.detail(sorts, groupTaskId, pageSize, pageNum);
        return ResponseResult.success(pageResult);
    }
}
