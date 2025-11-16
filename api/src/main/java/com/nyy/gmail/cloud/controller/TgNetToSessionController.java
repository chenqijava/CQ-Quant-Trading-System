package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.model.dto.TgNetToSessionReqDto;
import com.nyy.gmail.cloud.service.TgNetToSessionService;
import com.nyy.gmail.cloud.utils.TgNetToSessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;

@Slf4j
@RestController
@RequiredPermission(MenuType.tgNetToSession)
@RequestMapping({"/api/tgNetToSession"})
public class TgNetToSessionController {

    @Value("${gateway.tgnetBaseUrl}")
    private String tgnetBaseUrl;

    @PostConstruct
    public void init () {
        TgNetToSessionUtil.BASE_URL = this.tgnetBaseUrl;
    }

    @Autowired
    private TgNetToSessionService tgNetToSessionService;
    // 新增
    @PostMapping({"/save"})
    public Result save(@RequestBody(required = false) TgNetToSessionReqDto reqDTO) {
        tgNetToSessionService.save(reqDTO, Session.currentSession().userID);
        return ResponseResult.success();
    }

    // 列表
    // 2. 列表
    @PostMapping({"/{pageSize}/{pageNum}"})
    public Result<PageResult<GroupTask>> list(@PathVariable Integer pageSize, @PathVariable Integer pageNum,
                                              @RequestBody(required = false) Params params) {
        if (params.getFilters() == null) {
            params.setFilters(new HashMap<>());
        }
        params.getFilters().put("userID", Session.currentSession().userID);
        params.getFilters().put("type", TaskTypesEnums.TgNetToSession.getCode());

        PageResult<GroupTask> pageResult = tgNetToSessionService.findByPagination(params, pageSize, pageNum);
        pageResult.getData().forEach(e -> {
            e.setIds(null);
        });
        return ResponseResult.success(pageResult);
    }

    // 下载

    // 结束

    // 3. 删除
    @PostMapping({"/delete"})
    public Result delete(@RequestBody(required = false) IdsListDTO data) {
        tgNetToSessionService.delete(data);
        return ResponseResult.success();
    }

    // 5. 结束任务
    @PostMapping({"/stop"})
    public Result stop(@RequestBody(required = false)IdsListDTO data) {
        tgNetToSessionService.stop(data);
        return ResponseResult.success();
    }
}
