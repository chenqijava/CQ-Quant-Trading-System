package com.nyy.gmail.cloud.framework;

import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.framework.dto.AddTaskDomainDto;
import com.nyy.gmail.cloud.framework.mq.SubTaskMQProducer;
import com.nyy.gmail.cloud.mq.entity.TaskMessage;
import com.nyy.gmail.cloud.utils.UUIDUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TaskSchedulingService {

    @Autowired
    private SubTaskMQProducer subTaskMQProducer;

    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;

    public void apiAddTaskByDomain (AddTaskDomainDto domainDto) {
        if (domainDto == null) {
            return;
        }
        TaskMessage taskMessage = new TaskMessage();
        taskMessage.setAccid(domainDto.getAccid());
        taskMessage.setTraceId(UUIDUtils.get32UUId());
        SubTask subTask = new SubTask();
        BeanUtils.copyProperties(domainDto, subTask);
        taskMessage.setSubTask(subTask);

        subTaskMQProducer.sendMessage(taskMessage);
    }

    public void loopFindSubTask() {
        Map<String, Object> map = Map.of("status", "init");
        PageResult<SubTask> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(SubTask.class)
                .filters(map)
                .sorter(Map.of("_id", 1))
                .pageSize(1)
                .page(1)
                .build());
        long total = pageResult.getTotal();
        String minId = null;
        String maxId = null;
        if (total > 0) {
            minId = pageResult.getData().getFirst().get_id();
            pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                    .builder(SubTask.class)
                    .filters(Map.of("status", "init", "_id", Map.of("$gte", minId)))
                    .sorter(Map.of("_id", -1))
                    .pageSize(1)
                    .page(1)
                    .build());
            total = pageResult.getTotal();
            if (total > 0) {
                maxId = pageResult.getData().getFirst().get_id();
            }
        }

        int maxFindNum = 20000;

        Map<String, Object> filter = new HashMap<>();
        filter.put("status", "init");
        Map<String, Object> _id = new HashMap<>();
        if (StringUtils.isNotEmpty(minId)) {
            _id.put("$gte", minId);
            filter.put("_id", _id);
        }
        if (StringUtils.isNotEmpty(maxId)) {
            _id.put("$lte", maxId);
            filter.put("_id", _id);
        }

        while (true) {
            pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                    .builder(SubTask.class)
                    .filters(filter)
                    .sorter(Map.of("_id", 1))
                    .pageSize(maxFindNum)
                    .page(1)
                    .build());
            for (SubTask subTask : pageResult.getData()) {
                AddTaskDomainDto addTaskDomainDto = new AddTaskDomainDto();
//                addTaskDomainDto.set_id(subTask.get_id());
//                addTaskDomainDto.setType(subTask.getType());
//                addTaskDomainDto.setAccid(subTask.getAccid());
//                addTaskDomainDto.setGroupTaskId(subTask.getGroupTaskId());
//                addTaskDomainDto.setExecuteTime(subTask.getExecuteTime());
//                addTaskDomainDto.setCheckParams(subTask.getCheckParams());
//                addTaskDomainDto.setUpdateTime(subTask.getUpdateTime());
//                addTaskDomainDto.setParams(subTask.getParams());
//                addTaskDomainDto.setUserID(subTask.getUserID());
//                addTaskDomainDto.setStatus(subTask.getStatus());
//                addTaskDomainDto.setCreateTime(subTask.getCreateTime());
//                addTaskDomainDto.setResult(subTask.getResult());
//                addTaskDomainDto.setExecuteType(subTask.getExecuteType());
                BeanUtils.copyProperties(subTask, addTaskDomainDto);
                apiAddTaskByDomain(addTaskDomainDto);
            }
            if (pageResult.getData().size() < maxFindNum) {
                break;
            }
            _id.put("$gt", pageResult.getData().getLast().get_id());
            filter.put("_id", _id);
        }
    }
}
