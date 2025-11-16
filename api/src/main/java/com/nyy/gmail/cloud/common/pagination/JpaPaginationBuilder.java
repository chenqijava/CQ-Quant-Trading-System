package com.nyy.gmail.cloud.common.pagination;

import java.util.Map;

import lombok.Getter;

@Getter
public class JpaPaginationBuilder<T> {
    private Map<String, Object> filters = Map.of();
    private Map<String, Integer> sorter = Map.of();
    private Integer pageSize = 10;
    private Integer page = 1;
    private Class<T> entityClass;
    private boolean filterUserID = true;
    private boolean enableLog = true;

    public static <T> JpaPaginationBuilder<T> builder(Class<T> entityClass) {
        return new JpaPaginationBuilder<>(entityClass);
    }

    public JpaPaginationBuilder(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public JpaPaginationBuilder<T> filters(Map<String, Object> filters) {
        this.filters = filters;
        return this;
    }

    public JpaPaginationBuilder<T> sorter(Map<String, Integer> sorter) {
        this.sorter = sorter;
        return this;
    }

    public JpaPaginationBuilder<T> pageSize(Integer pageSize) {
        if (pageSize != null && pageSize > 0) {
            this.pageSize = pageSize;
        }
        return this;
    }

    public JpaPaginationBuilder<T> page(Integer page) {
        if (page != null && page > 0) {
            this.page = page;
        }
        return this;
    }

    public JpaPaginationBuilder<T> entityClass(Class<T> entityClass) {
        this.entityClass = entityClass;
        return this;
    }

    public JpaPaginationBuilder<T> filterUserID(boolean filterUserID) {
        this.filterUserID = filterUserID;
        return this;
    }

    public JpaPaginationBuilder<T> enableLog(boolean enableLog) {
        this.enableLog = enableLog;
        return this;
    }

    public JpaPaginationBuilder<T> build() {
        return this;
    } 
}
