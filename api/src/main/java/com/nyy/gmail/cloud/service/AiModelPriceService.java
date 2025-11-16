package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.AiModelPrice;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.repository.mongo.AiModelPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AiModelPriceService {

    @Autowired
    private AiModelPriceRepository aiModelPriceRepository;

    public PageResult<AiModelPrice> findByPagination(Params params, int pageSize, int page, String userID) {
        return aiModelPriceRepository.findByPagination(params, pageSize, page);
    }

    public void update(AiModelPrice reqDTO) {
        AiModelPrice aiModelPrice = aiModelPriceRepository.findById(reqDTO.get_id());
        if (aiModelPrice != null) {
            aiModelPrice.setCountPrice(reqDTO.getCountPrice());
            aiModelPrice.setOutputTokenPrice(reqDTO.getOutputTokenPrice());
            aiModelPrice.setInputTokenPrice(reqDTO.getInputTokenPrice());
            aiModelPrice.setStatus(reqDTO.getStatus());
            aiModelPrice.setType(reqDTO.getType());
            aiModelPrice.setSortNo(reqDTO.getSortNo());
            aiModelPriceRepository.save(aiModelPrice);
        }
    }

    public void delete(IdsListDTO reqDTO) {
        for (String id : reqDTO.getIds()) {
            aiModelPriceRepository.deleteById(id);
        }
    }

    public void add(AiModelPrice reqDTO) {
        AiModelPrice aiModelPrice = aiModelPriceRepository.findByModel(reqDTO.getModel());
        if (aiModelPrice == null) {
            aiModelPrice = new AiModelPrice();
            aiModelPrice.setCountPrice(reqDTO.getCountPrice());
            aiModelPrice.setOutputTokenPrice(reqDTO.getOutputTokenPrice());
            aiModelPrice.setInputTokenPrice(reqDTO.getInputTokenPrice());
            aiModelPrice.setStatus(reqDTO.getStatus());
            aiModelPrice.setType(reqDTO.getType());
            aiModelPrice.setSortNo(reqDTO.getSortNo());
            aiModelPrice.setCreateTime(new Date());
            aiModelPrice.setUserID(Constants.ADMIN_USER_ID);
            aiModelPrice.setModel(reqDTO.getModel());
            aiModelPriceRepository.save(aiModelPrice);
        } else {
            throw new CommonException(ResultCode.DATA_IS_EXISTED);
        }
    }
}
