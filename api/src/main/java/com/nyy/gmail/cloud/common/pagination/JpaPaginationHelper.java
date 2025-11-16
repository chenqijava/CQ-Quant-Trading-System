package com.nyy.gmail.cloud.common.pagination;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Component;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.constants.Constants;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JpaPaginationHelper {
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$");
    private static final List<String> STRING_REGEX_MATCH_KEYS = List.of();

    @Autowired
    private ApplicationContext applicationContext;
    
    private Repositories repositories;

    private Repositories getRepositories() {
        if (repositories == null) {
            repositories = new Repositories(applicationContext);
        }
        return repositories;
    }

    @SuppressWarnings("unchecked")
    public <T> PageResult<T> query(JpaPaginationBuilder<T> builder) {
        Specification<T> spec = buildSpecification(builder);
        Pageable pageable = createPageable(builder);

        Object repository = getRepositories().getRepositoryFor(builder.getEntityClass()).orElseThrow();
        JpaSpecificationExecutor<T> specExecutor = (JpaSpecificationExecutor<T>) repository;

        Page<T> page = specExecutor.findAll(spec, pageable);

        return createPageResult(builder, page.getTotalElements(), page.getTotalPages(), page.getContent());
    }

    private <T> Specification<T> buildSpecification(JpaPaginationBuilder<T> builder) {
        return (root, query, cb) -> {
            List<Predicate> predicates = buildPredicates(root, cb, builder);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * 构建查询条件
     */
    @SuppressWarnings("unchecked")
    private <T> List<Predicate> buildPredicates(Root<T> root, CriteriaBuilder cb, JpaPaginationBuilder<T> builder) {
        List<Predicate> predicates = new ArrayList<>();
        
        // 处理过滤条件
        builder.getFilters().forEach((key, value) -> {
            if (value != null) {
                if (value instanceof Map) {
                    handleMapCondition(predicates, root, cb, key, (Map<String, Object>) value);
                } else if (value instanceof String) {
                    handleStringCondition(predicates, root, cb, key, (String) value);
                } else if (value instanceof Number || value instanceof Boolean) {
                    predicates.add(cb.equal(root.get(key), value));
                } else if (value instanceof Collection) {
                    predicates.add(root.get(key).in((Collection<?>) value));
                }
            }
        });
        
        // 处理用户ID过滤
        if (builder.isFilterUserID() && Session.currentSession() != null) {
            String userId = Session.currentSession().getUserID();
            if (userId != null && !Constants.ADMIN_USER_ID.equals(userId)) {
                predicates.add(cb.equal(root.get("userID"), userId));
            }
        }
        
        return predicates;
    }
    
    /**
     * 创建分页结果
     */
    private <T> PageResult<T> createPageResult(JpaPaginationBuilder<?> builder, long total, int totalPages, List<T> data) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setPageNum(builder.getPage());
        pageResult.setPageSize(builder.getPageSize());
        pageResult.setTotal(total);
        pageResult.setPages(totalPages);
        pageResult.setData(data);
        return pageResult;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> void handleMapCondition(List<Predicate> predicates, Root<T> root, CriteriaBuilder cb, String key, Map<String, Object> condition) {
        Path<Object> path = root.get(key);
        
        condition.forEach((operator, operatorValue) -> {
            Object value = parseDateIfNecessary(operatorValue);
            
            switch (operator) {
                case "$eq":
                    predicates.add(cb.equal(path, value));
                    break;
                case "$ne":
                    predicates.add(cb.notEqual(path, value));
                    break;
                case "$gt":
                    predicates.add(cb.greaterThan(path.as(Comparable.class), (Comparable) value));
                    break;
                case "$gte":
                    predicates.add(cb.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) value));
                    break;
                case "$lt":
                    predicates.add(cb.lessThan(path.as(Comparable.class), (Comparable) value));
                    break;
                case "$lte":
                    predicates.add(cb.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) value));
                    break;
                case "$in":
                    predicates.add(path.in((Collection<?>) value));
                    break;
                case "$nin":
                    predicates.add(cb.not(path.in((Collection<?>) value)));
                    break;
                case "$like":
                    predicates.add(cb.like(cb.lower(path.as(String.class)), 
                            "%" + value.toString().toLowerCase() + "%"));
                    break;
            }
        });
    }

    private <T> void handleStringCondition(List<Predicate> predicates, Root<T> root, CriteriaBuilder cb, String key, String value) {
        if (STRING_REGEX_MATCH_KEYS.contains(key)) {
            // 模糊查询，不区分大小写
            predicates.add(cb.like(cb.lower(root.get(key).as(String.class)), 
                    "%" + value.toLowerCase() + "%"));
        } else {
            predicates.add(cb.equal(root.get(key), value));
        }
    }

    private Pageable createPageable(JpaPaginationBuilder<?> builder) {
        Sort sort = createSort(builder);
        if (sort != null) {
            return PageRequest.of(builder.getPage() - 1, builder.getPageSize(), sort);
        } else {
            return PageRequest.of(builder.getPage() - 1, builder.getPageSize());
        }
    }

    private Sort createSort(JpaPaginationBuilder<?> builder) {
        if (builder.getSorter() == null || builder.getSorter().isEmpty()) {
            return null;
        }
        
        List<Sort.Order> orders = new ArrayList<>();
        builder.getSorter().forEach((field, direction) -> {
            Sort.Direction dir = direction == 1 ? Sort.Direction.ASC : Sort.Direction.DESC;
            orders.add(new Sort.Order(dir, field));
        });
        
        return Sort.by(orders);
    }

    private static Object parseDateIfNecessary(Object value) {
        if (value instanceof String && ISO_DATE_PATTERN.matcher(value.toString()).matches()) {
            return Date.from(Instant.parse(value.toString()));
        }
        return value;
    }
}
