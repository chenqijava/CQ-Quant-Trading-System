package com.nyy.gmail.cloud.repository.mongo.custom;

import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.GroupTaskStatusEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface GroupTaskRepositoryCustom {
    List<GroupTask> findAllWaitPublishTask();

    PageResult<GroupTask> findByPagination(Integer pageSize, Integer pageNo, Map<String, Object> params);

    GroupTask findOneByOrderId(String orderId);

    void deleteImageRecognition();

    List<GroupTask> findImageRecognition();

    void updatePublishStatus(String id, String init, String status);
}

@Slf4j
@Repository
class GroupTaskRepositoryCustomImpl implements GroupTaskRepositoryCustom {

    @Resource
    private MongoTemplate mongoTemplate;

    // waitPublish的任务提前发布的时间
    private int aheadScheduleTime = 60 * 1000;

    @Resource
    private MongoPaginationHelper mongoPaginationHelper;

    @Override
    public List<GroupTask> findAllWaitPublishTask() {
        Query query = new Query();
        /* 如何状态待发布，且执行时间到了  或者 状态不等于待发布     发布状态是 init */
        query.addCriteria(Criteria.where("publishStatus").is("init").orOperator(
                Criteria.where("status").ne("waitPublish"),
                Criteria.where("status").is("waitPublish").and("executeTime").lt(new Date(new Date().getTime() + aheadScheduleTime))));
        query.with(Sort.by(Sort.Direction.ASC, "createTime"));
        List<GroupTask> groupTasks = mongoTemplate.find(query, GroupTask.class);
        return groupTasks;
    }

    @Override
    public PageResult<GroupTask> findByPagination(Integer pageSize, Integer pageNo, Map<String, Object> params) {
        PageResult<GroupTask> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(GroupTask.class).enableLog(false)
                .filters(params)
                .sorter(Map.of("createTime", -1, "_id", -1))
                .pageSize(pageSize)
                .page(pageNo)
                .build());
        return pageResult;
    }

    @Override
    public GroupTask findOneByOrderId(String orderId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("params.orderId").is(orderId).and("type").is(TaskTypesEnums.AccountExport.getCode()).and("userID").is(Constants.ADMIN_USER_ID));
        return mongoTemplate.findOne(query, GroupTask.class);
    }

    @Override
    public void deleteImageRecognition() {
        Query query = new Query();
        query.addCriteria(Criteria.where("type").is(TaskTypesEnums.ImageRecognition.getCode()).and("status").is(GroupTaskStatusEnums.success.getCode()).and("createTime").lt(new Date(new Date().getTime() - 24 * 60 * 60 * 1000)));
        mongoTemplate.remove(query, GroupTask.class);
    }

    @Override
    public List<GroupTask> findImageRecognition() {
        Query query = new Query();
        query.addCriteria(Criteria.where("type").is(TaskTypesEnums.ImageRecognition.getCode()).and("status").is(GroupTaskStatusEnums.success.getCode()).and("createTime").lt(new Date(new Date().getTime() - 24 * 60 * 60 * 1000)));
        query.fields().include("_id");
        return mongoTemplate.find(query, GroupTask.class);
    }

    @Override
    public void updatePublishStatus(String id, String init, String status) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("publishStatus", init);
        update.set("status", status);
        mongoTemplate.updateFirst(query, update, GroupTask.class);
    }
}