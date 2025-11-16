package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.AiModelPrice;
import com.nyy.gmail.cloud.enums.AiModelEnums;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.service.AiModelPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/aiModelPrice"})
public class AiModelPriceController {

    @Autowired
    private AiModelPriceService aiModelPriceService;

    // list
    @GetMapping("/model/list")
    public Result<List<String>> modelList() {
        List<String> list = Arrays.stream(AiModelEnums.values()).map(AiModelEnums::getCode).toList();
        return ResponseResult.success(list);
    }


    // list
    @PostMapping("/list/{pageSize}/{page}")
    public Result<PageResult<AiModelPrice>> list(@PathVariable int pageSize, @PathVariable int page, @RequestBody(required = false) Params params) {
        params.getFilters().put("userID", Session.currentSession().userID);
        PageResult<AiModelPrice> pageResult = aiModelPriceService.findByPagination(params, pageSize, page, Session.currentSession().userID);
        return ResponseResult.success(pageResult);
    }

    // update
    @PostMapping("/update")
    public Result update(@RequestBody(required = false) AiModelPrice reqDTO) {
        reqDTO.setUserID(Session.currentSession().userID);
        aiModelPriceService.update(reqDTO);
        return ResponseResult.success();
    }

    // delete
    @PostMapping("/delete")
    public Result delete(@RequestBody(required = false) IdsListDTO reqDTO) {
        aiModelPriceService.delete(reqDTO);
        return ResponseResult.success();
    }

    // save
    @PostMapping("/add")
    public Result add(@RequestBody(required = false) AiModelPrice reqDTO) {
        aiModelPriceService.add(reqDTO);
        return ResponseResult.success();
    }

}
