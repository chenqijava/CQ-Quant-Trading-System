package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.Check2FA;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.pagination.JpaPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.JpaPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mysql.ApiKey;
import com.nyy.gmail.cloud.model.dto.ApiKeyReqDTO;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.service.ApiKeyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredPermission(MenuType.apiKey)
@RequestMapping({"/api/apiKey"})
public class ApiKeyController {
    @Resource
    private JpaPaginationHelper jpaPaginationHelper;

    @Resource
    private ApiKeyService apiKeyService;

    // list
    @PostMapping("/list/{pageSize}/{page}")
    public Result<PageResult<ApiKey>> list(@PathVariable int pageSize, @PathVariable int page, @RequestBody(required = false) Params params) {
        params.getFilters().put("userID", Session.currentSession().userID);
        PageResult<ApiKey> pageResult = jpaPaginationHelper.query(JpaPaginationBuilder
                .builder(ApiKey.class)
                .filters(params.getFilters())
                .sorter(params.getSorter())
                .pageSize(pageSize)
                .page(page)
                .build());
        pageResult.getData().forEach(e -> {
            e.setApiSecret("");
        });
        return ResponseResult.success(pageResult);
    }

    // update
    @PostMapping("/update")
    public Result update(@RequestBody(required = false) ApiKeyReqDTO reqDTO) {
        reqDTO.setUserID(Session.currentSession().userID);
        apiKeyService.update(reqDTO);
        return ResponseResult.success();
    }

    // delete
    @Check2FA
    @PostMapping("/delete")
    public Result delete(@RequestBody(required = false) ApiKeyReqDTO reqDTO) {
        reqDTO.setUserID(Session.currentSession().userID);
        apiKeyService.delete(reqDTO);
        return ResponseResult.success();
    }

    // save
    @PostMapping("/add")
    @Check2FA
    public Result<ApiKey> add(@RequestBody(required = false) ApiKeyReqDTO reqDTO) {
        ApiKey apiKey = apiKeyService.add(reqDTO);
        return ResponseResult.success(apiKey);
    }

}
