package com.nyy.gmail.cloud.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.configuration.PathConfig;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.GroupTaskStatusEnums;
import com.nyy.gmail.cloud.enums.GroupTaskUserActionEnums;
import com.nyy.gmail.cloud.enums.SubTaskStatusEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.StatusGroupResult;
import com.nyy.gmail.cloud.mq.entity.TaskMessage;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TaskUtil {

    @Autowired
    private PathConfig pathConfig;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private RedissonClient redissonClient;

    public List<String> getAddData(Map params, int maxInsertCount) {
        if (!params.containsKey("addMethod")) {
            return new ArrayList<>();
        }
        if (params.get("addMethod").equals("1") || params.get("addMethod").equals("3")) {
            int begin = params.containsKey("pos") ? (int) params.get("pos") : 0;
            addData2addDatas(params);
            List<String> addDatas = ((List<String>)params.get("addDatas"));
            addDatas = addDatas.subList(begin, maxInsertCount > 0 ? Math.min(begin + maxInsertCount, addDatas.size()) : addDatas.size());
            params.put("pos", begin + addDatas.size());
            return addDatas;
        } else if (params.get("addMethod").equals("2")) {
            Path filepath = getFilePath(JSON.parseObject(JSON.toJSONString(params)));
            if (filepath != null) {
                Pair<List<String>, Long> listLongPair = FileUtils.readFileLines(filepath, params.containsKey("pos") ? (int) params.get("pos") : 0, maxInsertCount);
                params.put("pos", listLongPair.getRight().intValue());
                return listLongPair.getLeft();
            }
        }
        return new ArrayList<>();
    }

    private Path getFilePath(JSONObject params) {
        if (params.containsKey("basename")) {
            return Path.of(pathConfig.getUploadDir(), params.get("basename").toString());
        } else if (params.containsKey("filepath")) {
            return Path.of(pathConfig.getResBak(), params.get("filepath").toString());
        } else if (params.containsKey("uploadFile")) {
            return getFilePath(params.getJSONArray("uploadFile").getJSONObject(0).getJSONObject("response"));
        }
        return null;
    }

    private void addData2addDatas(Map params) {
        if (params.containsKey("addDatas") && params.get("addDatas") instanceof String) {
            String addDatas = params.get("addDatas").toString();
            params.put("addDatas", Arrays.stream(addDatas.split("\n")).map(String::trim).filter(StringUtils::isNotEmpty).toList());
        }
    }

    public SubTask newTaskModel(GroupTask groupTask, Map checkParams, Map params, Date executeTime) {
        SubTask subTask = new SubTask();
        subTask.setGroupTaskId(groupTask.get_id());
        subTask.setUserID(groupTask.getUserID());
        subTask.setType(groupTask.getType());
        subTask.setStatus(SubTaskStatusEnums.processing.getCode());
        subTask.setCheckParams(checkParams);
        subTask.setParams(params);
        subTask.setNeedFeedback(groupTask.getNeedFeedback());
        subTask.setExecuteTime(executeTime == null ? groupTask.getExecuteTime() : executeTime);
        subTask.setExecuteType(groupTask.getExecuteType());
        subTask.setCreateTime(new Date());
        return subTask;
    }

    public void finishForceTaskByPublish(GroupTask groupTask) {
        try {
            for (int i = 0; i < 50; i++) {
                List<SubTask> subTaskList = subTaskRepository.findByUserIDEqualsAndGroupTaskIdEqualsAndStatusIn(groupTask.getUserID(), groupTask.get_id(), List.of(SubTaskStatusEnums.init.getCode(), SubTaskStatusEnums.processing.getCode()), 1, 2000);
                if (subTaskList.size() <= 0) {
                    checkGroupCount(groupTask);
                    break;
                }
                log.info("{} : {} finishForceTask count: {}", groupTask.getType(), groupTask.get_id(), subTaskList.size());
                Update update = new Update();
                update.set("status", SubTaskStatusEnums.failed.getCode());
                update.set("result", Map.of("msg", "任务已经结束"));
                subTaskRepository.updateByIdInAndStatusIn(subTaskList.stream().map(SubTask::get_id).toList(), List.of(SubTaskStatusEnums.processing.getCode(), SubTaskStatusEnums.init.getCode()), update);
            }
        } catch (Exception e) {
            log.error("id: {} type: {} finishForceTaskByPublish error: {}", groupTask.get_id(), groupTask.getType(), e.getMessage());
        } finally {
            log.info("id: {} type: {} finish finishForceTaskByPublish", groupTask.get_id(), groupTask.getType());
        }
    }

    private void checkGroupCount(GroupTask groupTask) {
        if (!groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode()) || groupTask.getSuccess() + groupTask.getFailed() != groupTask.getTotal()) {
            List<StatusGroupResult> statusGroupResults = subTaskRepository.aggregateByGroupTaskIdEqualsGroupByStatus(groupTask.get_id());
            long success = 0;
            long failed = 0;
            for (StatusGroupResult e : statusGroupResults) {
                switch (e.getId()) {
                    case "success":
                        success = e.getNum();
                        break;
                    default:
                        failed += e.getNum();
                }
            }
            groupTask.setSuccess(success);
            groupTask.setFailed(failed);
            groupTask.setStatus(GroupTaskStatusEnums.success.getCode());
        }
        groupTaskRepository.save(groupTask);
    }

    public SubTask persistTaskModel(String id, GroupTask gt, Map checkParams, Map params, Date executeTime) {
        return _persistTaskModel(id, newTaskModel(gt, checkParams, params, executeTime));
    }

    private SubTask _persistTaskModel(String id, SubTask subTask) {
        subTask.setStatus(SubTaskStatusEnums.init.getCode());
        subTask.setAccid(id);
        subTask = subTaskRepository.insert(subTask);
        return subTask;
    }

    public TaskMessage domain2mqTask(String accid, SubTask subTask, Map extendParams) {
        TaskMessage message = new TaskMessage();
        message.setTraceId(UUIDUtils.get32UUId());
        message.setSubTask(subTask);
        message.setAccid(StringUtils.isNotEmpty(accid) ? accid : subTask.getAccid());
        message.setExtendParam(extendParams);
        return message;
    }

    public TaskMessage domain2mqTask(SubTask subTask) {
        return domain2mqTask(null, subTask, null);
    }

    public TaskMessage domain2mqTask(String accid, SubTask subTask) {
        return domain2mqTask(accid, subTask, null);
    }

    public void distributeAccount(Account account, SubTask subTask) {
        subTask.setAccid(account.get_id());
        if(subTask.getParams()!=null) {
            subTask.getParams().put("sendPhone", account.getPhone());
        }else {
            subTask.setParams(Map.of("sendPhone", account.getPhone()));
        }

        subTask.setStatus(SubTaskStatusEnums.init.getCode());
        subTask.setUpdateTime(new Date());
        subTaskRepository.save(subTask);
    }

    public void submitGroupByStatus(String groupTaskId, String status) {
        if (StringUtils.isEmpty(groupTaskId)) {
            return;
        }
        RLock lock = redissonClient.getLock("submitGroupByStatus_" + groupTaskId);
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    for (int i = 0; i < 5; i++) {
                        GroupTask groupTask = groupTaskRepository.findById(groupTaskId).orElse(null);
                        if (groupTask != null) {
                            switch (SubTaskStatusEnums.fromCode(status)) {
                                case null -> {}
                                case init,waitChange,processing -> {}
                                case doing,success -> {
                                    groupTask.setSuccess(groupTask.getSuccess() + 1);
                                }
                                default -> {
                                    groupTask.setFailed(groupTask.getFailed() + 1);
                                }
                            }
                            if (groupTask.getSuccess() + groupTask.getFailed() == groupTask.getTotal()) {
                                try {
                                    groupTask.setStatus(GroupTaskStatusEnums.success.getCode());
                                    groupTask.setFinishTime(new Date());
                                    groupTaskRepository.save(groupTask);
                                    break;
                                } catch (Exception e) {
                                }
                            } else if (groupTask.getSuccess() + groupTask.getFailed() > groupTask.getTotal()) {
                                try {
                                    checkGroupCount(groupTask);
                                    break;
                                } catch (Exception e) {
                                }
                            } else {
                                try {
                                    groupTaskRepository.save(groupTask);
                                    break;
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
        }
    }

    public GroupTask createGroupTask(List<String> ids, TaskTypesEnums typesEnums, Map params, String userID, String executeType, Date executeTime, boolean needFeedback) {
        if (StringUtils.isEmpty(userID)) {
            userID = Session.currentSession().userID;
        }
        if (StringUtils.isEmpty(userID)) {
            throw new CommonException(ResultCode.METHOD_NOT_ALLOWED);
        }
        if (!params.containsKey("taskDesc")) {
            params.put("taskDesc", typesEnums.getDescription());
        }
        GroupTask groupTask = new GroupTask();
        groupTask.setUserID(userID);
        groupTask.setIds(ids);
        groupTask.setType(typesEnums.getCode());
        groupTask.setDesc(params.getOrDefault("taskDesc", typesEnums.getDescription()).toString());
        groupTask.setStatus(executeType.equals("2") && executeTime != null ? GroupTaskStatusEnums.waitPublish.getCode() : GroupTaskStatusEnums.processing.getCode());
        groupTask.setParams(params);
        groupTask.setExecuteType(executeType);
        groupTask.setExecuteTime(executeTime == null ? new Date() : executeTime);
        groupTask.setNeedFeedback(needFeedback);
        groupTask.setCreateTime(new Date());
        groupTask.setTotal(params.containsKey("publishTotalCount") ? Long.parseLong(params.get("publishTotalCount").toString()) : (long)ids.size());
        groupTask.setPublishTotalCount(groupTask.getTotal());
        groupTask.setPublishedCount(0L);
        groupTask.setPublishStatus(needFeedback ? "success" : "init");
        groupTaskRepository.insert(groupTask);
        return groupTask;
    }

    public GroupTask createGroupTask(List<String> ids, TaskTypesEnums typesEnums, Map params, String userID, String executeType, Date executeTime) {
        return createGroupTask(ids, typesEnums, params, userID, executeType,executeTime,false);
    }

    public GroupTask createGroupTask(List<String> ids, TaskTypesEnums typesEnums, Map params, String userID) {
        return createGroupTask(ids, typesEnums, params, userID, "1", new Date());
    }

    public void stopGroupTask(List<String> ids, String userID) {
        for (String id : ids) {
            GroupTask groupTask = groupTaskRepository.findById(id).orElse(null);
            if (groupTask != null && groupTask.getUserID().equals(userID)) {
                groupTask.setUserAction(GroupTaskUserActionEnums.ForceFinish.getCode());
                groupTaskRepository.save(groupTask);
            }
        }
    }

    public void stopGroupTask(String id, String userID) {
        stopGroupTask(List.of(id), userID);
    }

    public TaskMessage buildTaskMessage(SubTask task, Account account) {
        TaskMessage taskMessage = new TaskMessage();
        taskMessage.setSubTask(task);
        taskMessage.setAccid(account.get_id());
        taskMessage.setTraceId(UUIDUtils.get32UUId());
        return taskMessage;
    }
}
