package com.nyy.gmail.cloud.controller;


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
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.model.dto.Socks5SetPlatformDTO;
import com.nyy.gmail.cloud.model.dto.Socks5SetUserDTO;
import com.nyy.gmail.cloud.model.result.AddIpResult;
import com.nyy.gmail.cloud.model.vo.Socks5SaveVO;
import com.nyy.gmail.cloud.model.vo.UploadSocks5VO;
import com.nyy.gmail.cloud.repository.mongo.RoleRepository;
import com.nyy.gmail.cloud.repository.mongo.Socks5Repository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.utils.ReflectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author liang sishuai
 * @version 1.0
 * @date 2021-09-16
 */
@Slf4j
@RestController
@RequiredPermission(MenuType.socks5)
@RequestMapping("/api/consumer/socks5")
public class Socks5Controller {

    @Autowired
    private Socks5Repository socks5Repository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;

    @PostMapping("/card/save")
    public Result<Socks5SaveVO> save(@RequestBody Socks5 socks5) {
        String userID = Session.currentSession().getUserID();
        try {
            if (StringUtils.isNotEmpty(socks5.get_id())) {
                socks5.setUserID(userID);
                socks5Repository.updateSocks5(socks5);

                Socks5SaveVO proxyAccountSaveVO = new Socks5SaveVO();
                proxyAccountSaveVO.set_id(socks5.get_id());
                Result<Socks5SaveVO> result = ResponseResult.success(proxyAccountSaveVO);
                return result;
            } else {
                socks5.setUserID(userID);
                socks5Repository.saveSocks5(socks5);

                Socks5SaveVO socks5SaveVO = new Socks5SaveVO();
                socks5SaveVO.set_id(socks5.get_id());
                Result<Socks5SaveVO> result = ResponseResult.success(socks5SaveVO);
                return result;
            }
        } catch (Exception e) {
            Result result = ResponseResult.failure(ResultCode.ERROR, null);
            result.setMessage(e.getMessage());
            return result;
        }
    }

    @PostMapping("/addIp")
    public Result<AddIpResult> addIp(@RequestBody List<Socks5> socks5List) {
        String userID = Session.currentSession().getUserID();
        if (CollectionUtils.isEmpty(socks5List)) {
            throw new CommonException("请填写数据");
        }
        AddIpResult addIpResult = socks5Repository.addIp(socks5List, userID);
        return ResponseResult.success(addIpResult);
    }

    @PostMapping("/addIpSingle")
    public Result<AddIpResult> addIpSingle(@RequestBody List<Socks5> socks5List) {

        String userID = Session.currentSession().getUserID();
        if (CollectionUtils.isEmpty(socks5List)) {
            throw new CommonException("请填写数据");
        }
        AddIpResult addIpResult = socks5Repository.addIpSingle(socks5List, userID);
        return ResponseResult.success(addIpResult);
    }

    @PostMapping("/delete")
    public Result<String> delete(@RequestBody List<String> ids) {
        String userID = Session.currentSession().getUserID();
        socks5Repository.deleteSocks5ByIds(ids, userID);

        Result<String> result = ResponseResult.success(null);
        return result;
    }

    @PostMapping("/setPlatform")
    public Result<String> setPlatform(@RequestBody Socks5SetPlatformDTO req) {
        String userID = Session.currentSession().getUserID();
        socks5Repository.updateSocksByPlatform(req.getIds(), userID, req.getPlatform());
        Result<String> result = ResponseResult.success(null);
        return result;
    }

    @PostMapping("/setUser")
    public Result<String> setUser(@RequestBody Socks5SetUserDTO req) {
        String userID = Session.currentSession().getUserID();
        socks5Repository.updateSocksByBelongUser(req.getIds(), userID, req.getUserID());
        Result<String> result = ResponseResult.success(null);
        return result;
    }

    @GetMapping("/card/{_id}")
    public Result<Socks5> get(@PathVariable String _id) {
        String userID = Session.currentSession().getUserID();
        Socks5 socks5 = socks5Repository.findSocks5ByIdUserID(_id, userID);
        Result<Socks5> result = ResponseResult.success(socks5);
        return result;
    }

//    @PostMapping("/{pageSize}/{page}")
//    public Result<PageResult<Socks5>> list(@PathVariable int pageSize, @PathVariable int page, @RequestBody(required = false) Socks5ListDTO socks5ListDTO) {
//        String userID = Session.currentSession().getUserID();
//
//        if (socks5ListDTO == null) {
//            socks5ListDTO = new Socks5ListDTO();
//        }
//        if (socks5ListDTO != null && socks5ListDTO.getFilters() != null) {
//            Map<String, String> filtersMap = socks5ListDTO.getFilters();
//            ReflectionUtil.setProperties(filtersMap, socks5ListDTO);
//        }
//        socks5ListDTO.setUserID(userID);
//
//        PageResult<Socks5> pageResult = socks5Repository.findSocks5PageList(socks5ListDTO, pageSize, page);//按id倒排序
//        Result<PageResult<Socks5>> result = ResponseResult.success(pageResult);
//        return result;
//    }

    @PostMapping("/{pageSize}/{page}")
    public Result<PageResult<Socks5>> list(@PathVariable int pageSize,
                                           @PathVariable int page,
                                           @RequestBody(required = false) Params params) {
        String userID = Session.currentSession().getUserID();
        params.getFilters().put("userID", userID);
        PageResult<Socks5> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(Socks5.class)
                .filters(params.getFilters())
                .sorter(params.getSorter())
                .pageSize(pageSize)
                .page(page)
                .build());
        pageResult.getData().forEach(e -> {
            e.setPort(0);
            e.setUsername("");
            e.setPassword("");
        });
        return ResponseResult.success(pageResult);
    }

    @PostMapping("/simpleList/{pageSize}/{page}")
    public Result<PageResult<Socks5>> simpleList(@PathVariable int pageSize, @PathVariable int page, @RequestBody(required = false) Params params) {
        String userID = Session.currentSession().getUserID();
        params.getFilters().put("userID", userID);
        PageResult<Socks5> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(Socks5.class)
                .filters(params.getFilters())
                .sorter(params.getSorter())
                .pageSize(pageSize)
                .page(page)
                .build());
        pageResult.getData().forEach(e -> {
            e.setPort(0);
            e.setUsername("");
            e.setPassword("");
        });
        return ResponseResult.success(pageResult);
    }

    private static final String[] SOCKS5_FIELD_NAMES = {"ip", "port", "username", "password", "platform", "areaCode"};

    @PostMapping("upload")
    public UploadSocks5VO upload(@RequestParam("file") MultipartFile file) throws IOException {
        String data = new String(file.getBytes(), StandardCharsets.UTF_8);
        data = data.replace("\r", "");
        int batchSize = 500;
        List<Socks5> entities = new ArrayList<>(batchSize);
        int repeat = 0;
        String[] lines = data.split("\n");
        int total = lines.length;
        for (String line : lines) {
            String[] sv = line.split(",");
            Map<String, String> socks5Map = new HashMap<>();
            socks5Map.put("desc", sv[0].trim());
            socks5Map.put("batchid", file.getOriginalFilename());

            // 赋值对应字段
            for (int i = 0; i < sv.length; i++) {
                if (sv[i].trim().length() > 0) {
                    socks5Map.put(SOCKS5_FIELD_NAMES[i], sv[i].trim());
                }
            }

            String userID = Session.currentSession().getUserID();
            socks5Map.put("userID", userID);
            if (!"admin".equals(userID)) {
                socks5Map.put("belongUser", userID);
            }

            Socks5 socks5 = new Socks5();
            socks5.setVersion(0L);
            ReflectionUtil.setProperties(socks5Map, socks5);

//            socks5Repository.saveSocks5(socks5);
            entities.add(socks5);
            if (entities.size() >= batchSize) {
                int success = socks5Repository.saveSocks5List(entities);
                repeat += entities.size() - success;
                entities = new ArrayList<>();
            }
        }
        if (!entities.isEmpty()) {
            int success = socks5Repository.saveSocks5List(entities);
            repeat += entities.size() - success;
        }

        UploadSocks5VO result = new UploadSocks5VO();
        result.setRepeat(repeat);
        result.setTotal(total);
        return result;
    }
}
