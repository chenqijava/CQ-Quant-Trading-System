package com.nyy.gmail.cloud.common.pagination;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.constants.Constants;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
public class MongoPaginationHelper {
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$");
    private static final Set<String> STRING_REGEX_MATCH_KEYS = Set.of(
            "accID", "errorMessage", "groupName", 
            "batchid", "ip", "platform", 
            "areaCode", "desc", "phone","description",
            "taskErrorMessage", "name", "email", "platformName", "orderNo", "orderDetailNo"
    );

    @Autowired
    private MongoTemplate mongoTemplate;
    
    public <T> PageResult<T> query(MongoPaginationBuilder<T> builder) {
        Query query = buildCommonQuery(builder);
        Pageable pageable = PageRequest.of(builder.getPage() - 1, builder.getPageSize());
        long countTimeStart = System.currentTimeMillis();
        long totalCount = mongoTemplate.count(query, builder.getEntityClass());
        long countTimeEnd = System.currentTimeMillis();
        List<T> data = mongoTemplate.find(query.with(pageable), builder.getEntityClass());
        long findTimeEnd = System.currentTimeMillis();

        if (builder.isEnableLog()) {
            log.info("[MongoPaginationHelper] [{}] [{}] [{}] ==> totalCount: {}, findCount: {}, countTime: {}ms, findTime: {}ms", 
                builder.getEntityClass().getSimpleName(), query.toString(), pageable.toString(), 
                totalCount, data.size(), countTimeEnd - countTimeStart, findTimeEnd - countTimeEnd
            );
        }

        PageResult<T> pageResult = new PageResult<>();
        pageResult.setPageNum(builder.getPage());
        pageResult.setPageSize(builder.getPageSize());
        pageResult.setTotal(totalCount);
        pageResult.setPages((int) Math.ceil((double) totalCount / builder.getPageSize()));
        pageResult.setData(data);
        return pageResult;
    }

    public <T> List<T> queryAll(MongoPaginationBuilder<T> builder) {
        return queryAll(builder, null);
    }

    public <T> List<T> queryAll(MongoPaginationBuilder<T> builder, Integer limit) {
        Query query = buildCommonQuery(builder);
        if (limit != null && limit > 0) {   
            query.limit(limit);
        }
        return mongoTemplate.find(query, builder.getEntityClass());
    }

    @SuppressWarnings("rawtypes")
    public <T> Query buildCommonQuery(MongoPaginationBuilder<T> builder) {
        Map<String, Criteria> criteriaMap = new HashMap<>();
        Optional.ofNullable(builder.getFilters()).ifPresent(filters -> filters.forEach((key, value) -> {
            if (value != null) {
                // log.info("key: {}, value: {}, valueType: {}", key, value, value.getClass());
                Criteria criteria = criteriaMap.computeIfAbsent(key, k -> Criteria.where(k));
                if (value instanceof Map) { //{ "field": {"$gt": 10} }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> condition = (Map<String, Object>) value;
                    condition.forEach((operator, operatorValue) -> {
                        switch (operator) {
                            case "$eq":
                                criteria.is(parseDateIfNecessary(operatorValue));
                                break;
                            case "$ne":
                                criteria.ne(parseDateIfNecessary(operatorValue));
                                break;
                            case "$gt":
                                criteria.gt(parseDateIfNecessary(operatorValue));
                                break;
                            case "$gte":
                                criteria.gte(parseDateIfNecessary(operatorValue));
                                break;
                            case "$lt":
                                criteria.lt(parseDateIfNecessary(operatorValue));
                                break;
                            case "$lte":
                                criteria.lte(parseDateIfNecessary(operatorValue));
                                break;
                            case "$in":
                                criteria.in((Collection)operatorValue);
                                break;
                            case "$nin":
                                criteria.nin(((Collection)operatorValue).toArray());
                                break;
                            case "$like":
                                criteria.regex(operatorValue.toString(), "i");
                                break;
                            case "$exists":
                                criteria.exists((Boolean) operatorValue);
                            default:
                                break;
                        }
                    });
                } else if (value instanceof String) {
                    if (STRING_REGEX_MATCH_KEYS.contains(key)) {
                        criteria.regex(value.toString(), "i"); // 不区分大小写，正则匹配提高匹配精度
                    } else {
                        criteria.is(value);
                    }
                } else if (value instanceof Number || value instanceof Boolean){
                    criteria.is(value);
                } else if (value instanceof Collection) {
                    criteria.in((Collection)value);
                } 
            }
        }));

        Query query = new Query();
        criteriaMap.values().forEach(query::addCriteria);

        if (builder.isFilterUserID() && Session.currentSession() != null) {
            String userID = Session.currentSession().getUserID();
            if (userID != null && !Constants.ADMIN_USER_ID.equals(userID)) {
                if (query.getQueryObject().get("userID") != null) {
                } else {
                    query.addCriteria(Criteria.where("userID").is(userID));
                }
            }
        }

        if (builder.getFieldsToInclude() != null && builder.getFieldsToInclude().length > 0) {
            query.fields().include(builder.getFieldsToInclude());
        }

        if (builder.getFieldsToExclude() != null && builder.getFieldsToExclude().length > 0) {
            query.fields().exclude(builder.getFieldsToExclude());
        }

        if (builder.getSorter() != null && builder.getSorter().size() > 0) {
            builder.getSorter().forEach((key, value) -> {
                Sort.Direction direction = (value == 1) ? Sort.Direction.ASC : Sort.Direction.DESC;
                query.with(Sort.by(direction, key));
            });
        }

        return query;
    }

    private static Object parseDateIfNecessary(Object value) {
        if (value instanceof String && ISO_DATE_PATTERN.matcher(value.toString()).matches()) {
            return Date.from(Instant.parse(value.toString()));
        }
        return value;
    }

}
