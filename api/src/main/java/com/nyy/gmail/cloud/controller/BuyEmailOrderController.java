package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.AccountPlatform;
import com.nyy.gmail.cloud.entity.mongo.BuyEmailOrder;
import com.nyy.gmail.cloud.entity.mongo.BuyEmailOrderDetail;
import com.nyy.gmail.cloud.entity.mongo.PlatformPrice;
import com.nyy.gmail.cloud.enums.BuyEmailDetailOrderStatus;
import com.nyy.gmail.cloud.model.dto.AccountPlatformReqDto;
import com.nyy.gmail.cloud.model.dto.BuyEmailOrderReqDto;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.repository.mongo.PlatformPriceRepository;
import com.nyy.gmail.cloud.service.AccountPlatformService;
import com.nyy.gmail.cloud.service.BuyEmailOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping({"/api/buyEmailOrder"})
public class BuyEmailOrderController {

    @Autowired
    private BuyEmailOrderService buyEmailOrderService;

    @Autowired
    private AccountPlatformService accountPlatformService;

    @Autowired
    private PlatformPriceRepository  platformPriceRepository;

    // 下单
    @RequiredPermission(MenuType.myOrder)
    @PostMapping({"/buy", "/open/buy"})
    public Result buy(@RequestBody(required = false) BuyEmailOrderReqDto reqDTO) {
        String orderId = buyEmailOrderService.buy(reqDTO, Session.currentSession().getUserID());
        return ResponseResult.success(orderId);
    }

    @RequiredPermission(MenuType.myOrderV2)
    @PostMapping(value = {"/buyV2", "/open/buyV2"})
    public Result buyV2(@RequestBody(required = false) BuyEmailOrderReqDto reqDTO) {
        String url = buyEmailOrderService.buyV2(reqDTO, Session.currentSession().getUserID());
        return ResponseResult.success(Map.of("downloadUrl", url));
    }

    // 列表
    @PostMapping("/list/{pageSize}/{page}")
    public Result<PageResult<BuyEmailOrder>> list(@PathVariable int pageSize, @PathVariable int page, @RequestBody(required = false) Params params) {
        String userID = Session.currentSession().getUserID();
        if (params.getFilters() == null) {
            params.setFilters(new HashMap<>());
        }
        if (!userID.equals(Constants.ADMIN_USER_ID)) {
            params.getFilters().put("userID", userID);
        }

        PageResult<BuyEmailOrder> orders = buyEmailOrderService.findByPagination(params, pageSize, page, Session.currentSession().getUserID());
        return ResponseResult.success(orders);
    }

    private static String formatEmail(String email) {
        // 拆分 email 为本地部分和域名部分
        String[] parts = email.split("@");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid email format");
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        // 保留第一个字母，其余字符用 '*' 填充
        String maskedLocalPart = localPart.charAt(0) + "*".repeat(localPart.length() - 1);

        // 返回格式化后的 email
        return maskedLocalPart + "@" + domainPart;
    }

    // 接码列表
    @PostMapping("/details/{pageSize}/{page}")
    public Result<PageResult<BuyEmailOrderDetail>> details(@PathVariable int pageSize, @PathVariable int page, @RequestBody(required = false) Params params) {
        String userID = Session.currentSession().getUserID();
        if (params.getFilters() == null) {
            params.setFilters(new HashMap<>());
        }
        if (!userID.equals(Constants.ADMIN_USER_ID)) {
            params.getFilters().put("userID", userID);
        }

        PageResult<BuyEmailOrderDetail> orders = buyEmailOrderService.findDetailsByPagination(params, pageSize, page, Session.currentSession().getUserID());
        orders.getData().forEach(e -> {
            if (!userID.equals(Constants.ADMIN_USER_ID)) {
                if (e.getStatus().equals(BuyEmailDetailOrderStatus.Expired.getCode()) || e.getStatus().equals(BuyEmailDetailOrderStatus.Released.getCode())) {
                    e.setEmail(formatEmail(e.getEmail()));
                }
            }
        });
        return ResponseResult.success(orders);
    }

    // 释放订单
    @PostMapping({"/release", "/open/release"})
    public Result release(@RequestBody(required = false) IdsListDTO reqDTO) {
        buyEmailOrderService.release(reqDTO.getId(), Session.currentSession().getUserID());
        return ResponseResult.success();
    }


    @PostMapping("/open/accountPlatformList")
    public Result<List<AccountPlatform>> accountPlatformList() {
        String userID = "admin";
        AccountPlatformReqDto data = new AccountPlatformReqDto();
        data.setFilters(new HashMap<>());
        data.setSorter(new HashMap<>());
        data.getFilters().put("userID", userID);
        data.getFilters().put("displayStatus", true);
        PageResult<AccountPlatform> accountByPagination = accountPlatformService.list(data, userID,  10000, 1);

        String uID = Session.currentSession().getUserID();
        if (StringUtils.isNotEmpty(uID)) {
            List<PlatformPrice> priceList = platformPriceRepository.findByUserID(uID);
            Map<String, BigDecimal> priceMap = priceList.stream().filter(e -> e.getPrice() != null).collect(Collectors.toMap(PlatformPrice::getPlatformId, PlatformPrice::getPrice, (e1, e2) -> e2));
            accountByPagination.getData().forEach(e -> {
                if (priceMap.containsKey(e.get_id())) {
                    e.setPrice(priceMap.get(e.get_id()));
                }
                e.setPattern(null);
                e.setEmailFrom(null);
                e.setCreateTime(null);
                e.setVersion(null);
                e.setUserID(null);
            });
        }

        return ResponseResult.success(accountByPagination.getData());
    }

    @PostMapping("/open/details/{orderNo}")
    public Result<List<BuyEmailOrderDetail>> openDetails(@PathVariable String orderNo) {
        String userID = Session.currentSession().getUserID();
        List<BuyEmailOrderDetail> details = buyEmailOrderService.openDetails(orderNo, userID);
        details.forEach(e -> {
            e.setAccid(null);
            e.setSubTaskId(null);
            e.setUserID(null);
            e.setPlatformId(null);
            e.setVersion(null);
        });
        return ResponseResult.success(details);
    }
}
