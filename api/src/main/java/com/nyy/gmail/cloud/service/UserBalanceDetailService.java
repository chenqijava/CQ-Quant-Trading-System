package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.BalanceDetail;
import com.nyy.gmail.cloud.model.dto.UserBalanceListDTO;
import com.nyy.gmail.cloud.repository.mongo.BalanceDetailRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class UserBalanceDetailService {

    @Resource
    private BalanceDetailRepository balanceDetailRepository;


    public PageResult<BalanceDetail> findUserByPagination(UserBalanceListDTO userBalanceListDTO, int pageSize, int page) {

        if (pageSize <= 0 || page <= 0) {
            // 非法
            return null;
        }

        return balanceDetailRepository.findUserByPagination(userBalanceListDTO, pageSize, page);
    }
}
