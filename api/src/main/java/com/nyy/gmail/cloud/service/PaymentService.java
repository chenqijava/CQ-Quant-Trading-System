package com.nyy.gmail.cloud.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.exception.NotifyException;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.entity.mysql.PaymentOrder;
import com.nyy.gmail.cloud.enums.BillCateTypeEnums;
import com.nyy.gmail.cloud.enums.BillExpenseTypeEnums;
import com.nyy.gmail.cloud.enums.PaymentStatusEnums;
import com.nyy.gmail.cloud.enums.WalletTypeEnums;
import com.nyy.gmail.cloud.model.dto.payment.PaymentNotifyDTO;
import com.nyy.gmail.cloud.model.result.ChannelPaymentResult;
import com.nyy.gmail.cloud.model.vo.payment.PaymentOrderVO;
import com.nyy.gmail.cloud.repository.mongo.BalanceDetailRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.repository.mysql.PaymentOrderRepository;
import com.nyy.gmail.cloud.utils.QRCodeBase64Util;
import com.nyy.gmail.cloud.utils.SignGenerator;
import com.nyy.gmail.cloud.utils.UUIDUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PaymentService {

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private ParamsService paramsService;

    @Resource
    private BalanceDetailRepository balanceDetailRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private RedissonClient redissonClient;

    @Value("${payment.apikey}")
    private String apiKey;

    @Value("${payment.url}")
    private String url;

    @Value("${payment.secretkey}")
    private String secret;

    public Result createPayment(String userID, BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0 || amount.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID);
        }
        String rechargeMethodStr = paramsService.getParams("account.rechargeMethod", null, null).toString();
        JSONArray jsonArray = JSONArray.parseArray(rechargeMethodStr);
        BigDecimal sendAmount = BigDecimal.ZERO;
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject.getString("status").equals("enable")) {
                if (jsonObject.getBigDecimal("value").compareTo(amount) <= 0) {
                    sendAmount = jsonObject.getBigDecimal("send");
                } else {
                    break;
                }
            }
        }

        PaymentOrder orderInfo = new PaymentOrder();
        orderInfo.setOrderNo(UUIDUtils.getPaymentOrderId());
        orderInfo.setUserId(userID);
        orderInfo.setAmount(amount);
        orderInfo.setStatus(PaymentStatusEnums.init.getCode());
        orderInfo.setWalletType(WalletTypeEnums.USDT_TRC20.getCode());
        orderInfo.setCreateTime(new Date());
        orderInfo.setSendAmount(sendAmount);
        paymentOrderRepository.save(orderInfo);

        Result<ChannelPaymentResult> result = sendPayment(Map.of("merchantOrderNo", orderInfo.getOrderNo(), "amount", orderInfo.getAmount().toString()));
        if (ResultCode.SUCCESS.getCode().equals(result.getCode())) {
            ChannelPaymentResult cpr = result.getData();
            orderInfo.setChannelOrderNo(cpr.getOrderNo());
            orderInfo.setAddress(cpr.getAddress());
            orderInfo.setAmount(new BigDecimal(cpr.getAmount()));
            orderInfo.setStatus(PaymentStatusEnums.processing.getCode());
            orderInfo.setTimeOutMillis(cpr.getTimeoutMillis());
            paymentOrderRepository.save(orderInfo);

            Long dt = cpr.getTimeoutMillis() - new Date().getTime();
            PaymentOrderVO vo = new PaymentOrderVO();
            vo.setAddress(cpr.getAddress());
            vo.setOrderNo(orderInfo.getOrderNo());
            vo.setAmount(cpr.getAmount());
            vo.setWalletType(WalletTypeEnums.USDT_TRC20.getDescription());
            vo.setDeadTime(dt / (1000 * 60));
            return ResponseResult.success(vo);
        }
        return result;
    }

    public Result<ChannelPaymentResult> sendPayment(Map<String, Object> params) {
        OkHttpClient httpClient = OkHttpClientFactory.getDefaultClient();
        ;

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String jsonBody = JSON.toJSONString(params);

        RequestBody body = RequestBody.create(jsonBody, mediaType);

        Request request = null;
        String payUrl = url;
        Request.Builder builder = new Request.Builder().url(payUrl);

        builder.addHeader("X-API-KEY", apiKey);
        builder.addHeader("X-SIGN", SignGenerator.generateSign(params, secret));
        request = builder.post(body).build();

        log.info("api{},params: {}", payUrl, JSON.toJSONString(params));

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                String respStr = response.body().string();
                JSONObject res = JSON.parseObject(respStr);
                int code = res.getIntValue("code");
                if (1 == code) {
                    ChannelPaymentResult cpr = res.getObject("data", ChannelPaymentResult.class);
                    return ResponseResult.success(cpr);
                }
                return ResponseResult.failure(code, res.getString("message"));
            } else {
                log.error("api: {}  Request failed http status code: {}", payUrl, response.code());
            }
        } catch (IOException e) {
            log.error("api: {}  Request failed error: {}", payUrl, e.getMessage());
        }

        return ResponseResult.failure(ResultCode.ERROR);
    }

    public Result nofityPaymentResult(PaymentNotifyDTO dto) {

        if (dto == null || !StringUtils.hasText(dto.getMerchant_trade_no()) || !StringUtils.hasText(dto.getOrder_status())) {
            return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID);
        }
        Map<String, Object> map = new HashMap<>();
        if (dto.getOrder_no() != null) {
            map.put("order_no", dto.getOrder_no());
        }
        if (dto.getAmount() != null) {
            map.put("amount", dto.getAmount());
        }
        if (dto.getPay_timestamp() != null) {
            map.put("pay_timestamp", dto.getPay_timestamp());
        }
        if (dto.getOrder_status() != null) {
            map.put("order_status", dto.getOrder_status());
        }
        if (dto.getMerchant_trade_no() != null) {
            map.put("merchant_trade_no", dto.getMerchant_trade_no());
        }

        String sign = SignGenerator.generateSign(map, secret);
        if (!sign.equals(dto.getSign())) {
            return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID);
        }

        PaymentOrder orderInfo = paymentOrderRepository.getPaymentOrdersByOrderNo(dto.getMerchant_trade_no());
        if (orderInfo == null) {
            return ResponseResult.failure(ResultCode.DATA_NOT_EXISTED);
        }
        RLock lock = redissonClient.getLock(orderInfo.getUserId() + "-charge");
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    if ("paid".equals(dto.getOrder_status()) && PaymentStatusEnums.processing.getCode().equals(orderInfo.getStatus())) {
                        orderInfo.setStatus(PaymentStatusEnums.success.getCode());
                        orderInfo.setUpdateTime(new Date());
                        paymentOrderRepository.save(orderInfo);
                        User user = userRepository.findOneByUserID_(orderInfo.getUserId());
                        if (user != null) {
                            try {
                                BigDecimal afterValue = user.getBalance().add(orderInfo.getAmount()).add(orderInfo.getSendAmount());
                                user.setBalance(afterValue);
                                user.setTotalRechargeBalance(user.getTotalRechargeBalance().add(orderInfo.getAmount()));
                                user.setTotalSendBalance(user.getTotalSendBalance().add(orderInfo.getSendAmount()));
                                userRepository.updateUser(user);
                                balanceDetailRepository.addUserBill("充值 "+orderInfo.getAmount() + (orderInfo.getSendAmount() != null && orderInfo.getSendAmount().compareTo(BigDecimal.ZERO) > 0 ? ", 赠送 " + orderInfo.getSendAmount() : ""), user.getUserID(),
                                        orderInfo.getAmount().add(orderInfo.getSendAmount()), afterValue, user.getName(), BillExpenseTypeEnums.IN, BillCateTypeEnums.CHARGE_ONLINE);
                                return ResponseResult.success();
                            } catch (OptimisticLockingFailureException oe) {
                                log.error("在线充订单：{} 收到通知，更新乐观锁异常：{}", user.getUserID(), orderInfo.getOrderNo(), oe);
                                throw new NotifyException();
                            }
                        }
                    }
                    return ResponseResult.success();
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
        }
        throw new NotifyException();
    }

    /**
     * 把支付中，超过时间戳的数据置为失败
     */
    public void timeOutToFail() {

        paymentOrderRepository.saveTimeOutToFail(PaymentStatusEnums.failed.getCode(), PaymentStatusEnums.processing.getCode(), new Date().getTime());
    }

    public Result<PaymentOrder> orderByOrderNo(String orderNo, String userID) {
        List<PaymentOrder> orders = paymentOrderRepository.findByOrderNoEqualsAndUserIdEquals(orderNo, userID);
        if (orders == null || orders.isEmpty()) {
            return ResponseResult.failure(ResultCode.DATA_NOT_EXISTED);
        }
        return ResponseResult.success(orders.getFirst());
    }
}
