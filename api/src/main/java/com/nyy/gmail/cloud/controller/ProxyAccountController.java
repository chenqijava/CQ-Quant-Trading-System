package com.nyy.gmail.cloud.controller;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.ProxyAccount;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.model.dto.ProxyAccountChangeEnableDTO;
import com.nyy.gmail.cloud.model.vo.ProxyAccountSaveVO;
import com.nyy.gmail.cloud.repository.mongo.ProxyAccountRepository;
import com.nyy.gmail.cloud.service.ProxyAccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author liang sishuai
 * @version 1.0
 * @date 2021-09-16
 */
@Slf4j
@RestController
@RequiredPermission(MenuType.proxyAccount)
@RequestMapping("/api/consumer/proxyAccount")
public class ProxyAccountController {

    @Autowired
    private ProxyAccountRepository proxyAccountRepository;

    @Autowired
    private ProxyAccountService proxyAccountService;

    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;

    @PostMapping("/card/save")
    public Result<ProxyAccountSaveVO> save(@RequestBody ProxyAccount proxyAccount) {
        String userID = Session.currentSession().getUserID();
        try {
            Result<ProxyAccountSaveVO> result;
            if (StringUtils.isNotEmpty(proxyAccount.get_id())) {
                ProxyAccount proxyAccountById = proxyAccountRepository.findProxyAccountById(proxyAccount.get_id());
                if (proxyAccountById == null || !proxyAccountById.getUserID().equals(userID)) {
                    throw new CommonException(ResultCode.PARAMS_IS_INVALID);
                }
                proxyAccountById.setDesc(proxyAccount.getDesc());
                proxyAccountById.setEnable(proxyAccount.getEnable());
                proxyAccountById.setPlatform(proxyAccount.getPlatform());
                proxyAccountById.setAccount(proxyAccount.getAccount());
                proxyAccountById.setToken(proxyAccount.getToken());
                proxyAccountById.setMaxVpsNum(proxyAccount.getMaxVpsNum());
                proxyAccountRepository.updateProxyAccount(proxyAccountById);

                ProxyAccountSaveVO proxyAccountSaveVO = new ProxyAccountSaveVO();
                proxyAccountSaveVO.set_id(proxyAccount.get_id());
                result= ResponseResult.success(proxyAccountSaveVO);
            } else {
                proxyAccount.setUserID(userID);
                proxyAccountRepository.saveProxyAccount(proxyAccount);

                ProxyAccountSaveVO proxyAccountSaveVO = new ProxyAccountSaveVO();
                proxyAccountSaveVO.set_id(proxyAccount.get_id());
                result = ResponseResult.success(proxyAccountSaveVO);
            }
            proxyAccountService.reloadActives();
            return result;
        } catch (Exception e) {
            Result result = ResponseResult.failure(ResultCode.ERROR, null);
            result.setMessage(e.getMessage());
            return result;
        }
    }

    @PostMapping("/delete")
    public Result<String> delete(@RequestBody List<String> ids) {
        String userID = Session.currentSession().getUserID();
        proxyAccountRepository.deleteProxyAccountByIds(ids, userID);
        proxyAccountService.reloadActives();
        Result<String> result = ResponseResult.success(null);
        return result;
    }

    @PostMapping("/changeEnable/{_id}")
    public Result<String> changeEnable(@PathVariable String _id, @RequestBody ProxyAccountChangeEnableDTO proxyAccountChangeEnableDTO) {
        String userID = Session.currentSession().getUserID();
        proxyAccountRepository.updateProxyAccountByUserIDIdEnable(userID, _id, proxyAccountChangeEnableDTO.getEnable());
        Result<String> result = ResponseResult.success(null);
        result.setMessage("修改成功");
        proxyAccountService.reloadActives();
        return result;
    }

    @GetMapping("/card/{_id}")
    public Result<ProxyAccount> get(@PathVariable String _id) {
        String userID = Session.currentSession().getUserID();
        ProxyAccount proxyAccount = proxyAccountRepository.findProxyAccountByIdUserID(_id, userID);
        Result<ProxyAccount> result = ResponseResult.success(proxyAccount);
        return result;
    }

    @PostMapping("/{pageSize}/{page}")
    public Result<PageResult<ProxyAccount>> list(@PathVariable int pageSize, @PathVariable int page, @RequestBody(required = false) Params params) {
        PageResult<ProxyAccount> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(ProxyAccount.class)
                .filters(params.getFilters())
                .sorter(params.getSorter())
                .pageSize(pageSize)
                .page(page)
                .filterUserID(true)
                .build());
        return ResponseResult.success(pageResult);
    }

    @PostMapping("/simpleList/{pageSize}/{page}")
    public Result<PageResult<ProxyAccount>> simpleList(@PathVariable int pageSize, @PathVariable int page, @RequestBody(required = false) Params params) {
        PageResult<ProxyAccount> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(ProxyAccount.class)
                .filters(params.getFilters())
                .sorter(params.getSorter())
                .pageSize(pageSize)
                .page(page)
                .build());
        return ResponseResult.success(pageResult);
    }

    private JSONArray PLATFORMS = new JSONArray();

    @GetMapping("/getIpPlatforms")
    public JSONArray getIpPlatforms() {
        String resp = proxyAccountService.getIpPlatforms();
        if (resp == null) {
            return PLATFORMS;
        }

        JSONObject json = JSON.parseObject(resp);
        PLATFORMS = json.getJSONArray("data");
        return PLATFORMS;
    }
}
