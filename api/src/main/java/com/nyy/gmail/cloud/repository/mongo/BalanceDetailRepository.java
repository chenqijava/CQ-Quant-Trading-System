package com.nyy.gmail.cloud.repository.mongo;

import com.nyy.gmail.cloud.common.pagination.MongoPageHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.BalanceDetail;
import com.nyy.gmail.cloud.enums.BillCateTypeEnums;
import com.nyy.gmail.cloud.enums.BillExpenseTypeEnums;
import com.nyy.gmail.cloud.model.dto.UserBalanceListDTO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Repository
public class BalanceDetailRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoPageHelper mongoPageHelper;

    public void save(BalanceDetail detail) {
        mongoTemplate.insert(detail);
    }

    public void update(BalanceDetail detail) {
        mongoTemplate.save(detail);
    }

    public void addUserBill(String desc, String userID, BigDecimal deltaValue, BigDecimal afterBalance, String name, BillExpenseTypeEnums expenseTypeEnum, BillCateTypeEnums cateTypeEnum) {
        addUserBill(desc, userID, deltaValue, afterBalance, name, expenseTypeEnum, cateTypeEnum, "");
    }

    public void addUserBill(String desc, String userID, BigDecimal deltaValue, BigDecimal afterBalance, String name, BillExpenseTypeEnums expenseTypeEnum, BillCateTypeEnums cateTypeEnum, String orderNo) {
        BalanceDetail balanceDetail = new BalanceDetail();
        balanceDetail.setDescription(desc);
        balanceDetail.setOperator(userID);
        balanceDetail.setType(cateTypeEnum.getCode());
        balanceDetail.setUserID(userID);
        balanceDetail.setCreateTime(new Date());
        balanceDetail.setBalance(afterBalance);
        balanceDetail.setValue(deltaValue);
        balanceDetail.setExpenseType(expenseTypeEnum.getCode());
        balanceDetail.setName(name);
        balanceDetail.setEmailOrderNo(orderNo);
        this.save(balanceDetail);
    }

    public PageResult<BalanceDetail> findUserByPagination(UserBalanceListDTO userBalanceListDTO, int pageSize, int page) {
        Query query = new Query();

        String userID = userBalanceListDTO.getUserID();
        if (userID != null) {
            Pattern pattern = Pattern.compile("^.*" + userID + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("userID").regex(pattern));
        }

        if (userBalanceListDTO.getFilters() != null) {
            Set<String> excludeKeys = new HashSet<>() {{
                add("description");
                add("userID");
            }};
            userBalanceListDTO.getFilters().forEach((key, value) -> {
                if (!excludeKeys.contains(key)) {
                    switch (key){
                        default:
                            query.addCriteria(Criteria.where(key).is(value));
                            break;
                    }

                }
            });
        }
        return mongoPageHelper.pageQuery(query, BalanceDetail.class, pageSize, page);
    }


    public BalanceDetail findByEmailOrderNo(String orderDetailNo) {
        Query query = new Query(Criteria.where("emailOrderNo").is(orderDetailNo));
        return mongoTemplate.findOne(query, BalanceDetail.class);
    }
}
