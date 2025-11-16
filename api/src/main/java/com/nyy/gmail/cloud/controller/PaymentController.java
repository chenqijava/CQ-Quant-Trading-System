package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mysql.PaymentOrder;
import com.nyy.gmail.cloud.model.dto.payment.PaymentDTO;
import com.nyy.gmail.cloud.model.dto.payment.PaymentNotifyDTO;
import com.nyy.gmail.cloud.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/payment"})
@Slf4j
public class PaymentController {


    @Autowired
    private PaymentService paymentService;

    @Autowired
    private HttpServletRequest request;

    @PostMapping("/create/order")
    public Result createPayment(@RequestBody PaymentDTO dto) {

        return paymentService.createPayment((String) request.getSession().getAttribute("userID"),dto.getAmount());
    }

    @PostMapping("/order/{orderNo}")
    public Result<PaymentOrder> orderByOrderNo(@PathVariable String orderNo) {
        return paymentService.orderByOrderNo(orderNo, Session.currentSession().getUserID());
    }

    @NoLogin
    @PostMapping("/notify")
    public ResponseEntity<Void> nofityPaymentResult(@RequestBody PaymentNotifyDTO dto){
        try {
            paymentService.nofityPaymentResult(dto);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            log.error("支付结果通知处理失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
