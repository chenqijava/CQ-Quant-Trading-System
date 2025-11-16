package com.nyy.gmail.cloud.controller;


import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.pagination.JpaPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.AccountGroup;
import com.nyy.gmail.cloud.enums.AccountGroupTypeEnums;
import com.nyy.gmail.cloud.model.dto.AccountGroupListDTO;
import com.nyy.gmail.cloud.model.dto.AccountGroupSaveDTO;
import com.nyy.gmail.cloud.service.AccountGroupService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@Slf4j
@RestController
//@RequiredPermission(MenuType.account)
@RequestMapping({"/api/consumer/accountGroup"})
public class AccountGroupController {

    @Autowired
    private AccountGroupService accountGroupService;

    @Resource
    private JpaPaginationHelper jpaPaginationHelper;

    @PostMapping("/save")
    public Result<AccountGroupSaveDTO> save(@RequestBody AccountGroup accountGroup) {
        String userID = Session.currentSession().getUserID();
        AccountGroupSaveDTO respDTO = accountGroupService.save(accountGroup, userID, AccountGroupTypeEnums.NORMAL);
        return ResponseResult.success(respDTO);
    }

    @PostMapping("/delete")
    public Result delete(@RequestBody List<String> ids) {
        String userID = Session.currentSession().getUserID();
        accountGroupService.delete(ids, userID);
        return ResponseResult.success(null);
    }

    @PostMapping("{pageSize}/{page}")
    public Result<PageResult<AccountGroup>> list(@PathVariable("pageSize") int pageSize, @PathVariable("page") int page,
                                                           @RequestBody(required = false) AccountGroupListDTO accountGroupListDTO) {
        String userID = Session.currentSession().getUserID();
        if(accountGroupListDTO.getFilters() == null) {
            accountGroupListDTO.setFilters(new HashMap<>());
        }
        accountGroupListDTO.getFilters().put("userID", userID);
        PageResult<AccountGroup> accountByPagination = accountGroupService.findAccountByPagination(accountGroupListDTO, userID,  pageSize, page);
        return ResponseResult.success(accountByPagination);
    }
}


