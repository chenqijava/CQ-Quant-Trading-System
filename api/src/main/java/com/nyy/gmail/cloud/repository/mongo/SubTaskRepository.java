package com.nyy.gmail.cloud.repository.mongo;

import com.nyy.gmail.cloud.common.pagination.MongoPageHelper;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.SubTaskStatusEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.StatusGroupResult;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class SubTaskRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private MongoPaginationHelper mongoPaginationHelper;

    @Resource
    private MongoPageHelper mongoPageHelper;

    public long countByGroupTaskIdEqualsAndStatusEquals(String id, String code) {
        Query query = new Query(Criteria.where("groupTaskId").is(id).and("status").is(code));
        return mongoTemplate.count(query, SubTask.class);
    }

    public void batchInsert(List<SubTask> processingTasks) {
        mongoTemplate.insertAll(processingTasks);
    }

    public List<SubTask> findByUserIDEqualsAndGroupTaskIdEqualsAndStatusIn(String userID, String id, List<String> code, int page, int pageSize) {
        Query query = new Query(Criteria.where("groupTaskId").is(id).and("status").in(code).and("userID").is(userID));
        query.skip((long) (page - 1) * pageSize);
        query.limit(pageSize);
        return mongoTemplate.find(query, SubTask.class);
    }

    public List<StatusGroupResult> aggregateByGroupTaskIdEqualsGroupByStatus(String id) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("groupTaskId").is(id)),
                Aggregation.group("status").count().as("num")
        );
        AggregationResults<StatusGroupResult> results = mongoTemplate.aggregate(
                aggregation,
                "subTask",         // MongoDB 集合名（不要写类名）
                StatusGroupResult.class
        );

        List<StatusGroupResult> groupedResults = results.getMappedResults();
        return groupedResults;
    }

    public void updateByIdInAndStatusIn(List<String> list, List<String> code, Update update) {
        Query query = new Query(Criteria.where("_id").in(list).and("status").in(code));
        mongoTemplate.updateMulti(query, update, SubTask.class);
    }

    public SubTask insert(SubTask subTask) {
        return mongoTemplate.insert(subTask);
    }

    public void save(SubTask subTask) {
        mongoTemplate.save(subTask);
    }

    public SubTask findById(String id) {
        return mongoTemplate.findById(id, SubTask.class);
    }

    public SubTask findByTmpId(String tmpId) {
        Query query = new Query(Criteria.where("tmpId").is(tmpId));
        return mongoTemplate.findOne(query, SubTask.class);
    }

    public List<SubTask> findByGroupTaskId(String groupId) {
        Query query = new Query(Criteria.where("groupTaskId").is(groupId));
        return mongoTemplate.find(query, SubTask.class);
    }

    public long countByGroupTaskIdEquals(String id) {
        Query query = new Query(Criteria.where("groupTaskId").is(id));
        return mongoTemplate.count(query, SubTask.class);
    }

    public List<SubTask> findByAccIdAndType(String id, String type) {
        Query query = new Query(Criteria.where("accid").is(id).and("type").is(type));
        return mongoTemplate.find(query, SubTask.class);
    }

    public PageResult<SubTask> findByPagination(Integer pageSize, Integer pageNo, Map<String, Object> params, Map<String, Integer> sorter) {
        if (sorter == null) {
            sorter = new HashMap<>();
        }
        sorter.put("_id", -1);
        PageResult<SubTask> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(SubTask.class).enableLog(false)
                .filters(params)
                .sorter(sorter)
                .pageSize(pageSize)
                .page(pageNo)
                .build());
        return pageResult;
    }

    public long countByAccIdAndTypeAndCreateTimeGatherThan(String id, String code, Date time) {
        Query query = new Query(Criteria.where("accid").is(id).and("type").is(code).and("createTime").gt(time));
        return mongoTemplate.count(query, SubTask.class);
    }

    public List<SubTask> findByTypeAndCreateTimeGatherThan(String code, Date time, int number) {
        Query query = new Query(Criteria.where("type").is(code).and("createTime").gt(time));
        query.with(Sort.by(Sort.Direction.ASC, "updateTime"));
        PageResult<SubTask> pageResult = mongoPageHelper.pageQuery(query, SubTask.class, number, 0);
        return pageResult.getData();
    }

    public void updateTimeByAccId(String id) {
        Update update = new Update();
        update.set("updateTime", new Date());
        update.set("tmpId", null);
        Query query = new Query(Criteria.where("accid").is(id).and("type").is(TaskTypesEnums.AccountExport.getCode()));
        mongoTemplate.updateMulti(query, update, SubTask.class);
    }

    public List<SubTask> findByTypeAndAccIdIn(String code, List<String> list, int number) {
        // 3天有效
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.DATE, -3);
        Query query = new Query(Criteria.where("type").is(code).and("accid").in(list).and("status").is(SubTaskStatusEnums.success.getCode()).and("tmpId").is("2").and("createTime").gt(instance.getTime()));
        query.with(Sort.by(Sort.Direction.ASC, "updateTime"));
        query.fields().include("accid");
        PageResult<SubTask> pageResult = mongoPageHelper.pageQuery(query, SubTask.class, number, 0);
        return pageResult.getData();
    }

    public List<SubTask> findRestart(String id, Date time) {
//        Update update = new Update();
//        update.set("updateTime", new Date());
//        update.set("status", SubTaskStatusEnums.processing.getCode());
//        update.set("accid", "");
//        update.set("params.apiKeyId", "");
        Query query = new Query(Criteria.where("groupTaskId").is(id).and("updateTime").lt(time).and("status").is(SubTaskStatusEnums.init.getCode()));
        return mongoTemplate.find(query, SubTask.class);
    }

    public List<SubTask> findByStatus(String code) {
        Query query = new Query(Criteria.where("status").is(code));
        query.fields().include("accid");
        return mongoTemplate.find(query, SubTask.class);
    }

    public long countSendEmailOneDayByAccid(String accid) {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.DATE, -1);

        Criteria timeCriteria = new Criteria().orOperator(
                Criteria.where("finishTime").gte(instance.getTime()),
                Criteria.where("createTime").gte(instance.getTime())
        );
        Query query = new Query(Criteria.where("accid").is(accid).and("type").is(TaskTypesEnums.BatchSendEmail.getCode()).and("status").ne(SubTaskStatusEnums.failed.getCode()).andOperator(timeCriteria));
        return mongoTemplate.count(query, SubTask.class);
    }

    public List<SubTask> findByIdIn(List<String> subTaskIds) {
        Query query = new Query(Criteria.where("_id").in(subTaskIds));
        return mongoTemplate.find(query, SubTask.class);
    }

    public void deleteImageRecognition(String id) {
        Query query = new Query(Criteria.where("groupTaskId").is(id));
        mongoTemplate.remove(query, SubTask.class);
    }

    public long countStatusIn(List<String> status) {
        Query query = new Query(Criteria.where("status").in(status));
        return mongoTemplate.count(query, SubTask.class);
    }

    public long countStatusInRecent(List<String> status, int i) {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE, -1 * i);
        Query query = new Query(Criteria.where("status").in(status).and("finishTime").gte(instance.getTime()));
        return mongoTemplate.count(query, SubTask.class);
    }

    public List<SubTask> findBySendGridTask(String uuid) {
        Query query = new Query(Criteria.where("result.msg").is(uuid).and("type").is(TaskTypesEnums.BatchSendEmail.getCode()).and("status").is(SubTaskStatusEnums.success.getCode()));
        return mongoTemplate.find(query, SubTask.class);
    }

    public long countSendEmailOneDayByAccidSuccess(String accid) {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.DATE, -1);

        Criteria timeCriteria = new Criteria().orOperator(
                Criteria.where("finishTime").gte(instance.getTime()),
                Criteria.where("createTime").gte(instance.getTime())
        );
        Query query = new Query(Criteria.where("accid").is(accid).and("type").is(TaskTypesEnums.BatchSendEmail.getCode()).and("status").is(SubTaskStatusEnums.success.getCode()).andOperator(timeCriteria));
        return mongoTemplate.count(query, SubTask.class);
    }

    public long countBySendEmailContent(String thisContent, String groupTaskId) {
        Query query = new Query(Criteria.where("groupTaskId").is(groupTaskId).and("type").is(TaskTypesEnums.BatchSendEmail.getCode()).and("status").is(SubTaskStatusEnums.init.getCode()).and("params.content").is(thisContent));
        return mongoTemplate.count(query, SubTask.class);
    }

    public long countBySendEmailTitle(String title, String groupTaskId) {
        Query query = new Query(Criteria.where("groupTaskId").is(groupTaskId).and("type").is(TaskTypesEnums.BatchSendEmail.getCode()).and("status").is(SubTaskStatusEnums.init.getCode()).and("params.title").is(title));
        return mongoTemplate.count(query, SubTask.class);
    }

    public List<SubTask> findAllByGroupTaskId(String groupTaskId) {
        Query query = new Query(Criteria.where("groupTaskId").is(groupTaskId));
        return mongoTemplate.find(query, SubTask.class);
    }

    public List<SubTask> findAllByGroupTaskIdAndStatus(String groupTaskId, String status) {
        Query query = new Query(Criteria.where("groupTaskId").is(groupTaskId).and("status").is(status));
        return mongoTemplate.find(query, SubTask.class);
    }

    public List<SubTask> findNormal(String groupTaskId) {
        Query query = new Query(Criteria.where("groupTaskId").is(groupTaskId).and("type").is(TaskTypesEnums.EmailCheckActive.getCode())
                .and("status").is(SubTaskStatusEnums.success.getCode()).and("result.msg").is("邮箱存在"));
        return mongoTemplate.find(query, SubTask.class);
    }

    public List<SubTask> findExcept(String groupTaskId) {
        Query query = new Query(Criteria.where("groupTaskId").is(groupTaskId).and("type").is(TaskTypesEnums.EmailCheckActive.getCode())
                .and("status").is(SubTaskStatusEnums.success.getCode()).and("result.msg").is("邮箱异常"));
        return mongoTemplate.find(query, SubTask.class);
    }

    public List<SubTask> findUnknown(String groupTaskId) {
        Query query = new Query(Criteria.where("groupTaskId").is(groupTaskId).and("type").is(TaskTypesEnums.EmailCheckActive.getCode())
                .and("status").is(SubTaskStatusEnums.failed.getCode()));
        return mongoTemplate.find(query, SubTask.class);
    }

    public List<SubTask> findNoExists(String groupTaskId) {
        Query query = new Query(Criteria.where("groupTaskId").is(groupTaskId).and("type").is(TaskTypesEnums.EmailCheckActive.getCode())
                .and("status").is(SubTaskStatusEnums.success.getCode()).and("result.msg").in(List.of("邮箱格式不合法，不存在","邮箱MX解析失败，不存在","SMTP验证失败，不存在")));
        return mongoTemplate.find(query, SubTask.class);
    }

    public void updateEvent(String id, String event, String number) {
        Update update = new Update();
        update.set("result." + event, number);
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.updateMulti(query, update, SubTask.class);
    }

    public void updateStatusAndResultById(String id, String status, String result) {
        Update update = new Update();
        update.set("status", status);
        update.set("result.msg", result);
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.updateMulti(query, update, SubTask.class);
    }

    public void updateStatusAndResultByIds(List<String> ids, String status, String result) {
        Update update = new Update();
        update.set("status", status);
        update.set("result.msg", result);
        Query query = new Query(Criteria.where("_id").in(ids));
        mongoTemplate.updateMulti(query, update, SubTask.class);
    }

    public long countSendEmailOneGroupTaskByAccid(String id, String accid) {
        Query query = new Query(Criteria.where("accid").is(accid).and("type").is(TaskTypesEnums.BatchSendEmail.getCode()).and("status").is(SubTaskStatusEnums.success.getCode()).and("groupTaskId").is(id));
        return mongoTemplate.count(query, SubTask.class);
    }

    public void updateResultLabel(String labels, String id, String email, String groupTaskId) {
        Update update = new Update();
        update.set("result.labels", labels);
        Query query = new Query(Criteria.where("accid").is(id).and("params.email").is(email).and("groupTaskId").is(groupTaskId));
        mongoTemplate.updateMulti(query, update, SubTask.class);
    }

    public List<SubTask> findByTypeAndFinishTimeAndStatusForSendEmail(String type, Date time, String status) {
        Query query = new Query(Criteria.where("type").is(type).and("finishTime").gte(time).and("status").is(status).and("result.labels").isNull());
        query.fields().include("params.email");
        return mongoTemplate.find(query, SubTask.class);
    }

    public long countSuccessAndNotLabels(String groupTaskId) {
        Query query = new Query(Criteria.where("groupTaskId").is(groupTaskId).and("status").is(SubTaskStatusEnums.success.getCode()).and("result.labels").isNull());
        return mongoTemplate.count(query, SubTask.class);
    }
}

