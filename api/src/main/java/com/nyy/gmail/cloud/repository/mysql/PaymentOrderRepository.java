package com.nyy.gmail.cloud.repository.mysql;

import com.nyy.gmail.cloud.entity.mysql.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PaymentOrderRepository  extends JpaRepository<PaymentOrder, String>, JpaSpecificationExecutor<PaymentOrder> {

    PaymentOrder getPaymentOrdersByOrderNo(String orderNo);

    @Modifying
    @Transactional
    @Query("update PaymentOrder t set t.status =:failStatus  where t.status =:processStatus  and t.timeOutMillis <:nowTime")
    void saveTimeOutToFail(@Param("failStatus") String failStatus,@Param("processStatus") String processStatus,@Param("nowTime") long time);

    List<PaymentOrder> findByOrderNoEqualsAndUserIdEquals(String orderNo, String orderNo1);
}
