package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.service.ParamsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author liang sishuai
 * @version 1.0
 * @date 2021-09-16
 */
@RestController
//@RequiredPermission(MenuType.adminRole)
@RequestMapping("/api/consumer/userParams")
public class UserParamsController {

    @Autowired
    private ParamsService paramsService;

    @GetMapping("/{paramCode}/get/{paramName}")
    public Result<Object> getUserConfig(@PathVariable String paramCode, @PathVariable String paramName) {
        String userID = Session.currentSession().getUserID();
        Object value = paramsService.getParams(paramCode + "." + paramName, null, userID);
        Result<Object> result = ResponseResult.success(value);
        return result;
    }

    @PostMapping("/{paramCode}/set/{paramName}")
    public Result<String> editUserConfig(@PathVariable String paramCode, @PathVariable String paramName, @RequestBody Map value) {
        String userID = Session.currentSession().getUserID();
        try {
            Object value_ = value.get("value");
            if (value_ == null) value_ = value;
            paramsService.editParams(paramCode + "." + paramName, value_, userID);

            Result<String> result = ResponseResult.success(null);
            result.setMessage("操作成功");
            return result;
        } catch (Exception e) {
            Result<String> result = ResponseResult.failure(ResultCode.ERROR);
            result.setMessage("操作失败," + e.getMessage());
            return result;
        }
    }
}
