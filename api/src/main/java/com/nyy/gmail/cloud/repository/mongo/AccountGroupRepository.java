package com.nyy.gmail.cloud.repository.mongo;

import com.mongodb.client.result.UpdateResult;
import com.nyy.gmail.cloud.common.pagination.MongoPageHelper;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.AccountGroup;
import com.nyy.gmail.cloud.enums.AccountGroupTypeEnums;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.model.dto.AccountGroupListDTO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Repository
public class AccountGroupRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoPageHelper mongoPageHelper;

    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;


    public void save(AccountGroup accountGroup) {
        mongoTemplate.insert(accountGroup);
    }

    public AccountGroup findByGroupNameAndUserID(String groupName, String userID) {
        Query query = new Query(Criteria.where("groupName").is(groupName).and("userID").is(userID));
        return mongoTemplate.findOne(query, AccountGroup.class);
    }

    public AccountGroup findByGroupTypeAndUserID(AccountGroupTypeEnums groupTypeEnums, String userID) {
        Query query = new Query(Criteria.where("groupType").is(groupTypeEnums.getCode()).and("userID").is(userID));
        return mongoTemplate.findOne(query, AccountGroup.class);
    }

    public void loginTimeoutChangeOnlineStatus() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -10);
        Query query = new Query(Criteria.where("onlineStatus").is(AccountOnlineStatus.WAITING_ONLINE.getCode()).and("changeOnlineStatusTime").lt(calendar.getTime()));
        Update update = new Update();
        update.set("onlineStatus", AccountOnlineStatus.OFFLINE.getCode());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
    }

    public PageResult<AccountGroup> findByPagination(AccountGroupListDTO accountGroupListDTO, int pageSize, int page) {
        return mongoPaginationHelper.query(MongoPaginationBuilder.builder(AccountGroup.class).filters(accountGroupListDTO.getFilters()).sorter(accountGroupListDTO.getSorter()).pageSize(pageSize).page(page).build());
    }

    public void deleteManyByIds(List<String> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        mongoTemplate.remove(query, AccountGroup.class);
    }

    public List<AccountGroup> findByOnlineStatus(List<String> status, String userID) {
        Query query = new Query(Criteria.where("userID").is(userID).and("onlineStatus").in(status));
        return mongoTemplate.find(query, AccountGroup.class);
    }

    public void deleteByUserID(String userID) {
        Query query = new Query(Criteria.where("userID").is(userID));
        mongoTemplate.remove(query, AccountGroup.class);
    }

    public boolean updateOnlineStatus(String id, String code) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("onlineStatus", code);
        update.set("changeOnlineStatusTime", new Date());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
        return updateResult.getModifiedCount() > 0;
    }

    public void update(Account account) {
        mongoTemplate.save(account);
    }

    public Account findByAccID(String uid) {
        Query query = new Query(Criteria.where("accID").is(uid));
        return mongoTemplate.findOne(query, Account.class);
    }

    public AccountGroup findById(String _id) {
        Query query = new Query(Criteria.where("_id").is(_id));
        return mongoTemplate.findOne(query, AccountGroup.class);
    }

    public List<Account> findByIdsInAndOnlineStatusIn(List<String> ids, List<String> strings) {
        Query query = new Query(Criteria.where("_id").in(ids).and("onlineStatus").in(strings));
        return mongoTemplate.find(query, Account.class);
    }

    public AccountGroup findByIdAndUserID(String _id, String userID) {
        Query query = new Query(Criteria.where("_id").is(_id).and("userID").is(userID));
        return mongoTemplate.findOne(query, AccountGroup.class);
    }

    public List<AccountGroup> findByIdListAndUserID(List<String> idList, String userID) {
        Query query = new Query(Criteria.where("_id").in(idList).and("userID").is(userID));
        return mongoTemplate.find(query, AccountGroup.class);
    }


    public List<AccountGroup> findIdListAndUserID(List<String> groupIdList, String userID) {
        Query query = new Query(Criteria.where("_id").in(groupIdList).and("userID").is(userID));
        return mongoTemplate.find(query, AccountGroup.class);
    }

    public boolean updateGroupName(String id, String groupName) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("groupName", groupName);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, AccountGroup.class);
        return updateResult.getModifiedCount() > 0;
    }

    public List<AccountGroup> findByUserID(String userID) {
        Query query = new Query(Criteria.where("userID").is(userID));
        return mongoTemplate.find(query, AccountGroup.class);
    }
}
