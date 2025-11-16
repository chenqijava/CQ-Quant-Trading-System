package com.nyy.gmail.cloud.common.pagination;

import lombok.Getter;
import java.util.Map;


@Getter
public class MongoPaginationBuilder<T> {
    private Map<String, Object> filters = Map.of();
    private Map<String, Integer> sorter = Map.of();
    private Integer pageSize = 10;
    private Integer page = 1;
    private String[] fieldsToInclude;
    private String[] fieldsToExclude;
    private Class<T> entityClass;
    private boolean filterUserID = true;
    private boolean enableLog = true;

    public static <T> MongoPaginationBuilder<T> builder(Class<T> entityClass) {
        return new MongoPaginationBuilder<>(entityClass);
    }

    public MongoPaginationBuilder(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public MongoPaginationBuilder<T> filters(Map<String, Object> filters) {
        this.filters = filters;
        return this;
    }

    public MongoPaginationBuilder<T> sorter(Map<String, Integer> sorter) {
        this.sorter = sorter;
        return this;
    }

    public MongoPaginationBuilder<T> pageSize(Integer pageSize) {
        if (pageSize != null && pageSize > 0) {
            this.pageSize = pageSize;
        }
        return this;
    }

    public MongoPaginationBuilder<T> page(Integer page) {
        if (page != null && page > 0) {
            this.page = page;
        }
        return this;
    }

    public MongoPaginationBuilder<T> fieldsToInclude(String... fieldsToInclude) {
        this.fieldsToInclude = fieldsToInclude;
        return this;
    }

    public MongoPaginationBuilder<T> fieldsToExclude(String... fieldsToExclude) {
        this.fieldsToExclude = fieldsToExclude;
        return this;
    }

    public MongoPaginationBuilder<T> entityClass(Class<T> entityClass) {
        this.entityClass = entityClass;
        return this;
    }

    public MongoPaginationBuilder<T> filterUserID(boolean filterUserID) {
        this.filterUserID = filterUserID;
        return this;
    }

    public MongoPaginationBuilder<T> enableLog(boolean enableLog) {
        this.enableLog = enableLog;
        return this;
    }

    public MongoPaginationBuilder<T> build() {
        return this;
    }
}
