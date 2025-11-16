package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.AccountPlatform;
import com.nyy.gmail.cloud.entity.mongo.PlatformPrice;
import com.nyy.gmail.cloud.model.dto.AccountPlatformReqDto;
import com.nyy.gmail.cloud.repository.mongo.PlatformPriceRepository;
import com.nyy.gmail.cloud.service.AccountPlatformService;
import com.nyy.gmail.cloud.utils.ReflectionUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping({"/api/platform", "/api/consumer/platform"})
public class AccountPlatformController {

    @Autowired
    private AccountPlatformService accountPlatformService;

    @Autowired
    private PlatformPriceRepository platformPriceRepository;

    @RequiredPermission(MenuType.platform)
    @PostMapping("/save")
    public Result save(@RequestBody AccountPlatform accountPlatform) {
        String userID = Session.currentSession().getUserID();
        accountPlatformService.save(accountPlatform, userID);
        return ResponseResult.success();
    }

    @RequiredPermission(MenuType.platform)
    @PostMapping("/delete")
    public Result delete(@RequestBody List<String> ids) {
        String userID = Session.currentSession().getUserID();
        accountPlatformService.delete(ids, userID);
        return ResponseResult.success(null);
    }

    @PostMapping("{pageSize}/{page}")
    public Result<PageResult<AccountPlatform>> list(@PathVariable("pageSize") int pageSize, @PathVariable("page") int page,
                                                 @RequestBody(required = false) AccountPlatformReqDto data) {
//        String userID = Session.currentSession().getUserID();
        String userID = "admin";
        data.getFilters().put("userID", userID);
        PageResult<AccountPlatform> accountByPagination = accountPlatformService.list(data, userID,  pageSize, page);
        return ResponseResult.success(accountByPagination);
    }

//    @NoLogin
    @PostMapping("/common/{pageSize}/{page}")
    public Result<PageResult<AccountPlatform>> commonList(@PathVariable("pageSize") int pageSize, @PathVariable("page") int page,
                                                    @RequestBody(required = false) AccountPlatformReqDto data) {
        String userID = "admin";
        data.getFilters().put("userID", userID);
        data.getFilters().put("displayStatus", true);
        PageResult<AccountPlatform> accountByPagination = accountPlatformService.list(data, userID,  pageSize, page);

        String uID = Session.currentSession().getUserID();
        if (StringUtils.isNotEmpty(uID)) {
            List<PlatformPrice> priceList = platformPriceRepository.findByUserID(uID);
            Map<String, BigDecimal> priceMap = priceList.stream().filter(e -> e.getPrice() != null).collect(Collectors.toMap(PlatformPrice::getPlatformId, PlatformPrice::getPrice, (e1, e2) -> e2));
            accountByPagination.getData().forEach(e -> {
                if (priceMap.containsKey(e.get_id())) {
                    e.setPrice(priceMap.get(e.get_id()));
                }
            });
        }

        return ResponseResult.success(accountByPagination);
    }

    @RequiredPermission(MenuType.platform)
    @PostMapping("export")
    public void export(HttpServletResponse response) throws IOException {
        String userID = Session.currentSession().getUserID();
        List<AccountPlatform> accountPlatforms = accountPlatformService.findAll(userID);

        List<String> lines = accountPlatforms.stream().map(e -> e.getName() + "----" + e.getEmailFrom() + "----" + e.getPattern() + "----" + e.getSortNo() + "----" + e.getPrice().toPlainString() + "----" + (e.getIcon() == null ? "" : e.getIcon())).toList();

        response.setHeader("Cache-Control", "public");

        ServletOutputStream outputStream = response.getOutputStream();
        byte[] buff = String.join("\n", lines).getBytes();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength(buff.length);
        response.setHeader("Content-Disposition", "attachment;filename=" + UriEncoder.encode("accountPlatform.txt"));
        outputStream.write(buff, 0, buff.length);
        outputStream.flush();
    }

    @RequiredPermission(MenuType.platform)
    @PostMapping("import")
    public Result importPlatform(@RequestParam("file") MultipartFile file) throws IOException {
        String data = new String(file.getBytes(), StandardCharsets.UTF_8);
        data = data.replace("\r", "");
        int batchSize = 500;
        List<AccountPlatform> entities = new ArrayList<>(batchSize);
        int repeat = 0;
        String[] lines = data.split("\n");
        int total = lines.length;
        for (String line : lines) {
            String[] sv = line.split("----", -1);
            Map<String, String> map = new HashMap<>();
            map.put("name", sv[0].trim());
            map.put("emailFrom", sv[1].trim());
            map.put("pattern", sv[2].trim());


            String userID = Session.currentSession().getUserID();
            map.put("userID", userID);

            AccountPlatform platform = new AccountPlatform();
            ReflectionUtil.setProperties(map, platform);
            platform.setCanUseAccountNumber(0);
            platform.setVersion(0L);
            platform.setCreateTime(new Date());
            platform.setSortNo(sv.length == 4 ? Integer.parseInt(sv[3]) : 0);
            platform.setPrice(new BigDecimal(sv.length == 5 ? sv[4] : "0"));
            platform.setIcon(sv.length == 6 ? sv[5] : "");
            platform.setDisplayStatus(false);

            entities.add(platform);
            if (entities.size() >= batchSize) {
                int success = accountPlatformService.saveBatch(entities);
                repeat += entities.size() - success;
                entities = new ArrayList<>();
            }
        }
        if (!entities.isEmpty()) {
            int success = accountPlatformService.saveBatch(entities);
            repeat += entities.size() - success;
        }

        return ResponseResult.success();
    }
}
