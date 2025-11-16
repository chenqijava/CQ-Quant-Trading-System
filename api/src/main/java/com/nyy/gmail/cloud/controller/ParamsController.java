package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.model.vo.ParamsVO;
import com.nyy.gmail.cloud.service.ParamsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author liang sishuai
 * @date 2021-09-16
 * @version 1.0
 */
@RestController
@RequestMapping({"/api/consumer/params", "/api/common/params"})
public class ParamsController {
    @Autowired
    private ParamsService paramsService;

    @NoLogin
    @GetMapping("/{paramCode}/get/{paramName}")
    public ParamsVO getConfig(@PathVariable String paramCode, @PathVariable String paramName){
        Object value = paramsService.getParams(paramCode + "." + paramName, null, null);
        ParamsVO paramsVO = new ParamsVO();
        paramsVO.setCode(ResultCode.SUCCESS.getCode());
        paramsVO.setValue(value);
        return paramsVO;
    }

    @RequiredPermission(MenuType.globalParams)
    @PostMapping("/{paramCode}/set/{paramName}")
    public Result<String> editConfig(@PathVariable String paramCode, @PathVariable String paramName, @RequestBody Map value){
        try {
            Object value_ = value.get("value");
            if(value_ == null) value_ = value;

            paramsService.editParams(paramCode + "." + paramName, value_, null);

            Result<String> result = ResponseResult.success(null);
            result.setMessage("操作成功");
            return result;
        }
        catch(Exception e) {
            Result<String> result = ResponseResult.failure(ResultCode.ERROR);
            result.setMessage("操作失败," + e.getMessage());
            return result;
        }
    }
}
