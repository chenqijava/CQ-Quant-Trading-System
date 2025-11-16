package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class SubTaskService {

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    public PageResult<GroupTask> list(Integer pageSize, Integer pageNo, Map<String, Object> params) {
        return groupTaskRepository.findByPagination(pageSize, pageNo, params);
    }

    public PageResult<SubTask> queryProcessDetail(Integer pageSize, Integer pageNo, String taskId, Map<String, Object> params, String userID) {
        params.put("groupTaskId", taskId);
        params.put("userID", userID);
        return subTaskRepository.findByPagination(pageSize, pageNo, params, null);
    }
}
