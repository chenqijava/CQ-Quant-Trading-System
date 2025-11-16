package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.GroupTaskStatusEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.SieveActiveReqDto;
import com.nyy.gmail.cloud.tasks.sieve.SieveActiveTask;
import com.nyy.gmail.cloud.tasks.sieve.SieveActiveTaskFactory;
import com.nyy.gmail.cloud.tasks.sieve.bo.SieveActiveGroupTaskParams;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class SieveActiveService {

    @Autowired
    private TaskUtil taskUtil;
    @Autowired
    private MongoTemplate mongoTemplate;

    public GroupTask save(SieveActiveReqDto reqDTO, String userID) throws IOException {
        File file = FileUtils.resPath.resolve(reqDTO.getFilepath()).toFile();
        int total = FileUtils.getFileNonEmptyLineCount(file);

        List<String> ids = Collections.emptyList();
        SieveActiveGroupTaskParams params = new SieveActiveGroupTaskParams()
                .setProject(Objects.requireNonNull(reqDTO.getProject()))
                .setTaskDesc(StringUtils.defaultString(reqDTO.getDesc()))
                .setPublishTotalCount(total)
                .setAddMethod("2")
                .setDataFilePath(reqDTO.getFilepath());
        return taskUtil.createGroupTask(ids, TaskTypesEnums.SieveActive, params.toMap(), userID, "2", new Date());
    }

    public int stop(List<String> ids, Boolean forceStop, String userID) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(ids).and("status").nin(GroupTaskStatusEnums.success.getCode(), GroupTaskStatusEnums.failed.getCode()));
        List<GroupTask> groupTasks = mongoTemplate.find(query, GroupTask.class);
        log.info("运营【{}】【forceStop:{}】手动取消任务,任务ID:【{}】,个数:【{}】,未完成任务个数:【{}】", userID, forceStop, ids, ids.size(), groupTasks.size());
        groupTasks.forEach(groupTask -> {
            SieveActiveTask taskBean = SieveActiveTaskFactory.instance.getTaskBean(groupTask);
            if (Boolean.TRUE.equals(forceStop)) {
                taskBean.forceStopTask(groupTask);
            } else {
                taskBean.stopTask(groupTask);
            }
        });
        return groupTasks.size();
    }

}
