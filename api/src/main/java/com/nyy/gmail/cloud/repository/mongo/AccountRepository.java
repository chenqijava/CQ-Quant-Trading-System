package com.nyy.gmail.cloud.repository.mongo;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.UpdateResult;
import com.nyy.gmail.cloud.common.pagination.MongoPageHelper;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.enums.AccountHasLoginSuccessTypeEnums;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.AccountOtherStatusTypeEnums;
import com.nyy.gmail.cloud.enums.AccountTypeEnums;
import com.nyy.gmail.cloud.model.dto.AccountListDTO;
import com.nyy.gmail.cloud.model.dto.GroupAccountStats;
import com.nyy.gmail.cloud.utils.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@Repository
public class AccountRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoPageHelper mongoPageHelper;

    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;

    public boolean updateSocks5Id(String id, String socks5Id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("socks5Id", socks5Id);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
        return updateResult.getModifiedCount() > 0;
    }

    public boolean updateSession(String id, String session) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("session", session);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
        return updateResult.getModifiedCount() > 0;
    }

    public boolean updateLoginSession(String id, String loginSession, String session, String phone, String accID) {
        if (StringUtils.isNotEmpty(accID)) {
            Query query2 = new Query(Criteria.where(accID).is(accID).and("_id").ne(id));
            long count = mongoTemplate.count(query2, Account.class);
            if (count > 0) {
                accID = null;
            }
        }

        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        if (StringUtils.isNotEmpty(session)) {
            update.set("session", session);
        }
        if (StringUtils.isNotEmpty(loginSession)) {
            update.set("loginSession", loginSession);
        }
        if (StringUtils.isNotEmpty(accID)) {
            update.set("accID", accID);
        }
        if (StringUtils.isNotEmpty(phone)) {
            update.set("phone", phone);
        }
        if (StringUtils.isNotEmpty(session) && StringUtils.isNotEmpty(loginSession)) {
            update.set("onlineStatus", AccountOnlineStatus.ONLINE.getCode());
        }
        if (StringUtils.isNotEmpty(loginSession)) {
            update.set("hasLoginSuccess", AccountHasLoginSuccessTypeEnums.YES.getCode());
            update.set("otherStatus", AccountOtherStatusTypeEnums.NORMAL.getCode());
        }

        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
        return updateResult.getModifiedCount() > 0;
    }

    public void save(Account account) {
        mongoTemplate.insert(account);
    }

    public Account findByIdAndUserID(String accid, String userID) {
        Query query = new Query(Criteria.where("_id").is(accid).and("userID").is(userID));
        return mongoTemplate.findOne(query, Account.class);
    }

    public List<Account> findByGroupIdList(List<String> groupIdList) {
        Query query = new Query(Criteria.where("groupID").in(groupIdList));
        return mongoTemplate.find(query, Account.class);
    }


    public List<Account> findByGroupIdListAndUserID(List<String> groupIdList, String userID) {
        Query query = new Query(Criteria.where("groupID").in(groupIdList).and("userID").is(userID));
        query.fields().include("_id", "groupID", "onlineStatus");
        return mongoTemplate.find(query, Account.class);
    }

    public List<GroupAccountStats> aggregateAccountStats(List<String> groupIds, String userId) {

        // 1. 过滤条件
        MatchOperation match = Aggregation.match(
                Criteria.where("groupID").in(groupIds)
                        .and("userID").is(userId)
        );

        // 2. 分组统计
        GroupOperation group = Aggregation.group("groupID")
                .count().as("total")
                .sum(ConditionalOperators.when(Criteria.where("onlineStatus").is(AccountOnlineStatus.ONLINE.getCode())).then(1).otherwise(0))
                .as("onlineTotal");

        // 3. 聚合管道
        Aggregation aggregation = Aggregation.newAggregation(match, group);

        // 4. 执行查询并映射结果
        return mongoTemplate.aggregate(aggregation, "account", GroupAccountStats.class).getMappedResults();
    }

    public void loginTimeoutChangeOnlineStatus() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -10);
        Query query = new Query(Criteria.where("onlineStatus").is(AccountOnlineStatus.WAITING_ONLINE.getCode()).and("changeOnlineStatusTime").lt(calendar.getTime()));
        Update update = new Update();
        update.set("onlineStatus", AccountOnlineStatus.OFFLINE.getCode());
        update.set("loginError", "超时未登录成功");
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
    }

    public PageResult<Account> findByPagination(AccountListDTO accountListDTO, int pageSize, int page) {
        if (accountListDTO.getFilters() != null) {
            if (accountListDTO.getFilters().containsKey("platform")) {
                String platform = accountListDTO.getFilters().get("platform").toString();
                accountListDTO.getFilters().remove("platform");
                accountListDTO.getFilters().put("usedPlatformIds", Map.of("$ne", platform));
            }

            if (accountListDTO.getFilters().containsKey("platform2")) {
                String platform = accountListDTO.getFilters().get("platform2").toString();
                accountListDTO.getFilters().remove("platform2");
                accountListDTO.getFilters().put("realUsedPlatformIds", platform);
            }


            if (accountListDTO.getFilters().containsKey("onlineStatus")) {
                String onlineStatus = accountListDTO.getFilters().get("onlineStatus").toString();
                if (!onlineStatus.equals(AccountOnlineStatus.ONLINE.getCode())) {
                    accountListDTO.getFilters().put("onlineStatus", Map.of("$ne", AccountOnlineStatus.ONLINE.getCode()));
                }
            }

            if (accountListDTO.getFilters().containsKey("createTimeRange")) {
                List<String> createTimeRange = (List<String>)accountListDTO.getFilters().get("createTimeRange");
                accountListDTO.getFilters().remove("createTimeRange");
                if (createTimeRange.size() == 2) {
                    accountListDTO.getFilters().put("createTime", Map.of(
                            "$gte", Objects.requireNonNull(DateUtil.getDateByFormat(createTimeRange.getFirst(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM)),
                            "$lte", Objects.requireNonNull(DateUtil.getDateByFormat(createTimeRange.getLast(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM))));
                }
            }
        }

        PageResult<Account> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(Account.class)
                .filterUserID(true)
                .filters(accountListDTO.getFilters())
                .sorter(accountListDTO.getSorter())
                .pageSize(pageSize)
                .page(page)
                .build());
        return pageResult;
    }

    public void deleteManyByIds(List<String> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        mongoTemplate.remove(query, Account.class);
    }

    public List<Account> findByOnlineStatus(List<String> status, String userID) {
        Query query = new Query(Criteria.where("userID").is(userID).and("onlineStatus").in(status));
        return mongoTemplate.find(query, Account.class);
    }

    public void deleteByUserID(String userID) {
        Query query = new Query(Criteria.where("userID").is(userID));
        mongoTemplate.remove(query, Account.class);
    }

    public boolean updateOnlineStatus(String id, String code, String loginError) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("onlineStatus", code);
        update.set("changeOnlineStatusTime", new Date());
        update.set("loginError", loginError);
        update.set("isCheck", true);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
        return updateResult.getModifiedCount() > 0;
    }

    public boolean updateOnlineStatusOtherStatus(String id, String code, String loginError, AccountOtherStatusTypeEnums accountOtherStatusTypeEnums) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("onlineStatus", code);
        update.set("changeOnlineStatusTime", new Date());
        update.set("loginError", loginError);
        update.set("otherStatus", accountOtherStatusTypeEnums.getCode());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
        return updateResult.getModifiedCount() > 0;
    }

    public boolean updateOnlineStatusOtherStatusV2(String id, String code, AccountOtherStatusTypeEnums accountOtherStatusTypeEnums) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("onlineStatus", code);
        update.set("changeOnlineStatusTime", new Date());
        update.set("otherStatus", accountOtherStatusTypeEnums.getCode());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
        return updateResult.getModifiedCount() > 0;
    }

    public boolean updateOtherStatusByIdList(List<String> idList,AccountOtherStatusTypeEnums accountOtherStatusTypeEnums) {
        Query query = new Query(Criteria.where("_id").in(idList));
        Update update = new Update();
        update.set("changeOnlineStatusTime", new Date());
        update.set("otherStatus", accountOtherStatusTypeEnums.getCode());
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

    public Account findById(String accid) {
        Query query = new Query(Criteria.where("_id").is(accid));
        return mongoTemplate.findOne(query, Account.class);
    }

    public List<Account> findByIdsInAndOnlineStatusIn(List<String> ids, List<String> strings) {
        Query query = new Query(Criteria.where("_id").in(ids).and("onlineStatus").in(strings));
        return mongoTemplate.find(query, Account.class);
    }

    public List<Account> findByIdsInAndOnlineStatusInAndUserID(List<String> ids, List<String> strings, String userID) {
        Query query = new Query(Criteria.where("_id").in(ids).and("onlineStatus").in(strings).and("userID").is(userID));
        return mongoTemplate.find(query, Account.class);
    }

    public List<Account> findBySocks5IdAndStatus(String socks5Id, String onlineStatus) {
        Query query = new Query(Criteria.where("socks5Id").is(socks5Id).and("onlineStatus").is(onlineStatus));
        return mongoTemplate.find(query, Account.class);
    }

    public List<Account> findBySocks5IdListAndStatus(List<String> socks5IdList, String onlineStatus) {
        Query query = new Query(Criteria.where("socks5Id").in(socks5IdList).and("onlineStatus").is(onlineStatus));
        return mongoTemplate.find(query, Account.class);
    }


    public void updateSessionAndServerId(String id, String session, String serverid) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("session", session);
        update.set("gatewayServerId", serverid);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
    }

    public List<Account> findByHasLoginSuccessStatusAndQrCodeExpiredTime(AccountHasLoginSuccessTypeEnums accountHasLoginSuccessTypeEnums) {
        Query query = new Query(Criteria.where
                ("hasLoginSuccess").is(accountHasLoginSuccessTypeEnums.getCode())
                .and("qrCodeExpiredTime").lt(System.currentTimeMillis())
        );
        return mongoTemplate.find(query, Account.class);
    }

    public void updateLastSendMessageTime(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("lastSendMsgTime", new Date());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
    }


    public Account findByPhone(String phone) {
        Query query = new Query(Criteria.where("phone").is(phone));
        return mongoTemplate.findOne(query, Account.class);
    }

    public List<Account> findByIds(List<String> ids) {

        Query query = new Query(Criteria.where("_id").in(ids));
        return mongoTemplate.find(query, Account.class);
    }

    public long countNoUsePlatformStock(List<String> ids, String platformId, String userID) {
        Query query = new Query(Criteria.where("usedPlatformIds").ne(platformId).and("openExportReceiveCode").is("1").and("type").nin(AccountTypeEnums.sendgrid.getCode()).and("onlineStatus").is(AccountOnlineStatus.ONLINE.getCode()).and("isCheck").is(true));
        if (!CollectionUtils.isEmpty(ids)) {
            query.addCriteria(Criteria.where("_id").in(ids));
        }
        if (StringUtils.isNotBlank(userID)) {
            query.addCriteria(Criteria.where("userID").is(userID));
        }

        return mongoTemplate.count(query, Account.class);
    }

    public List<Account> findByLastPullMessageTimeIsNull() {
        Query query = new Query(Criteria.where("lastPullMessageTime").isNull());
        return mongoTemplate.find(query, Account.class);
    }

    public List<Account> findByLastPullMessageTimeLessThanEqual(Date time, int number) {
        Query query = new Query(Criteria.where("lastPullMessageTime").lte(time).and("onlineStatus").is(AccountOnlineStatus.ONLINE.getCode()).and("type").ne(AccountTypeEnums.sendgrid.getCode()));
        query.with(Sort.by(Sort.Direction.ASC, "lastPullMessageTime"));
        PageResult<Account> pageResult = mongoPageHelper.pageQuery(query, Account.class, number, 0);
        return pageResult.getData();
    }

    public long countByUserID(String userID) {
        Query query = new Query(Criteria.where("userID").is(userID));
        return mongoTemplate.count(query, Account.class);
    }

    public long countByPlatformId(String id, String userID) {
        Query query = new Query(Criteria.where("usedPlatformIds").is(id).and("userID").is(userID));
        return mongoTemplate.count(query, Account.class);
    }

    public long countByUserIDAndOnlineStatus(String userID, String code) {
//        Query query = new Query(Criteria.where("onlineStatus").is(code).
//                and("openExportReceiveCode").is("1").
//                and("type").nin(AccountTypeEnums.sendgrid.getCode()).and("userID").is(userID).and("isCheck").is(true));
        Query query = new Query(Criteria.where("onlineStatus").is(code).
                and("openExportReceiveCode").is("1").
                and("type").nin(AccountTypeEnums.sendgrid.getCode()).and("isCheck").is(true));
        return mongoTemplate.count(query, Account.class);
    }

    public long countByPlatformIdAndOnlineStatus(String id, String userID, String code) {
//        Query query = new Query(Criteria.where("usedPlatformIds").is(id).and("userID").is(userID).and("onlineStatus").is(code).and("isCheck").is(true));
        Query query = new Query(Criteria.where("usedPlatformIds").is(id).and("openExportReceiveCode").is("1").and("onlineStatus").is(code).and("isCheck").is(true));
        return mongoTemplate.count(query, Account.class);
    }

    public List<Account> findAllByOnlineStatusOnlyId(String code) {
        Query query = new Query(Criteria.where("onlineStatus").is(code).and("isCheck").is(true).and("type").ne(AccountTypeEnums.sendgrid.getCode()));
        query.fields().include("_id");
        return mongoTemplate.find(query, Account.class);
    }

    public void updateTokenAndDeviceinfo(Account account) {
        Query query = new Query(Criteria.where("_id").is(account.get_id()));
        Update update = new Update();
        update.set("token", account.getToken());
        update.set("deviceinfo", account.getDeviceinfo());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
    }

    public void updateLastPullMessageTime(String id, Date lastPullMessageTime) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("lastPullMessageTime", lastPullMessageTime);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);

    }

    public void updateSocks5IdAndProxyIp(String id, String socks5Id, String proxyIp) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("socks5Id", socks5Id);
        update.set("proxyIp", proxyIp);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);

    }

    public void updateLoginSuccess(Account account) {
        Query query = new Query(Criteria.where("_id").is(account.get_id()));
        Update update = new Update();
        /**
         * account.setSession(result.getSession());
         *                         account.setDeviceinfo(result.getDeviceinfo());
         *                         account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
         *                         account.setChangeOnlineStatusTime(new Date());
         *                         account.setLoginError("");
         *                         account.setIsCheck(true);
         */
        update.set("session", account.getSession());
        update.set("deviceinfo", account.getDeviceinfo());
        update.set("onlineStatus", account.getOnlineStatus());
        update.set("changeOnlineStatusTime", new Date());
        update.set("loginError", "");
        update.set("isCheck", true);
        if (StringUtils.isNotEmpty(account.getAccID())) {
            update.set("accID", account.getAccID());
        }
        if (StringUtils.isNotEmpty(account.getEmail())) {
            update.set("email", account.getEmail());
        }
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
    }

    public List<Account> findAllIncludePart(String userID, AccountListDTO accountListDTO) {
        Criteria criteria = Criteria.where("userID").is(userID);

        if (accountListDTO.getFilters() != null) {
            if (accountListDTO.getFilters().containsKey("platform")) {
                String platform = accountListDTO.getFilters().get("platform").toString();
                criteria.and("usedPlatformIds").ne(platform);
            }

            if (accountListDTO.getFilters().containsKey("platform2")) {
                String platform = accountListDTO.getFilters().get("platform2").toString();
                criteria.and("realUsedPlatformIds").ne(platform);
            }


            if (accountListDTO.getFilters().containsKey("onlineStatus")) {
                String onlineStatus = accountListDTO.getFilters().get("onlineStatus").toString();
                if (!onlineStatus.equals(AccountOnlineStatus.ONLINE.getCode())) {
                    criteria.and("onlineStatus").ne(AccountOnlineStatus.ONLINE.getCode());
                } else {
                    criteria.and("onlineStatus").is(AccountOnlineStatus.ONLINE.getCode());
                }
            }

            if (accountListDTO.getFilters().containsKey("createTimeRange")) {
                List<String> createTimeRange = (List<String>)accountListDTO.getFilters().get("createTimeRange");
                accountListDTO.getFilters().remove("createTimeRange");
                if (createTimeRange.size() == 2) {
                    criteria.and("createTime").gte(Objects.requireNonNull(DateUtil.getDateByFormat(createTimeRange.getFirst(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM))).lte(Objects.requireNonNull(DateUtil.getDateByFormat(createTimeRange.getLast(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM)));
                }
            }

            if (accountListDTO.getFilters().containsKey("email")) {
                String email = accountListDTO.getFilters().get("email").toString();
                criteria.and("email").regex(email);
            }

            if (accountListDTO.getFilters().containsKey("phone")) {
                String phone = accountListDTO.getFilters().get("phone").toString();
                if (phone.equals("1")) {
                    criteria.and("phone").ne(null);
                } else if (phone.equals("0")) {
                    criteria.and("phone").is(null);
                }
            }

            if (accountListDTO.getFilters().containsKey("limitSendEmail")) {
                String limitSendEmail = accountListDTO.getFilters().get("limitSendEmail").toString();
                if (limitSendEmail.equals("0")) {
                    criteria.and("limitSendEmail").ne(true);
                } else if (limitSendEmail.equals("1")) {
                    criteria.and("limitSendEmail").is(true);
                }
            }
            if (accountListDTO.getFilters().containsKey("type")) {
                criteria.and("type").ne(AccountTypeEnums.sendgrid.getCode());
            }
        }
        Query query = new Query(criteria);
        query.fields().include("_id", "email", "password","phone", "used", "realUsedPlatformIds",
                "onlineStatus", "isCheck", "loginError", "changeOnlineStatusTime", "createTime",
                "sendEmailNumByDayDisplay", "sendEmailTotal", "limitSendEmail");
        return mongoTemplate.find(query, Account.class);
    }

    public void updateSendEmailNum(String id, long l, long l2) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("sendEmailNumByDay", l);
        update.set("sendEmailNumByDayDisplay", l2);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
    }

    public Account findByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        return mongoTemplate.findOne(query, Account.class);
    }

    public void updateLimitSendEmail(String accid) {
        Query query = new Query(Criteria.where("_id").is(accid));
        Update update = new Update();
        update.set("limitSendEmail", true);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
    }

    public void updateLimitSendEmail(String accid, boolean limitSendEmail) {
        Query query = new Query(Criteria.where("_id").is(accid));
        Update update = new Update();
        update.set("limitSendEmail", limitSendEmail);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Account.class);
    }

    public List<Account> findByApiKeysIn(List<String> apiKeys) {
        Query query = new Query(Criteria.where("sendGridApiKey").in(apiKeys));
        return mongoTemplate.find(query, Account.class);
    }

    public int saveBatch(List<Account> entities) {
        BulkOperations operations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Account.class);
        operations.insert(entities);
        try {
            BulkWriteResult result = operations.execute();
            log.info("表:【GoogleStudioApiKey】, 待插入记录条数:【{}】, 成功条数:【{}】", entities.size(), result.getInsertedCount());
            return result.getInsertedCount();
        } catch (BulkOperationException e) {
            BulkWriteResult result = e.getResult();
            log.info("表:【GoogleStudioApiKey】, 待插入记录条数:【{}】, 成功条数:【{}】", entities.size(), result.getInsertedCount());
            return result.getInsertedCount();
        } catch (Exception e) {
            log.error("表:【GoogleStudioApiKey】, 数据个数:【{}】 失败", entities.size(), e);
        }
        return 0;
    }

    public void resetSendGridOnlineStatus() {
        Query query = new Query(Criteria.where("type").is(AccountTypeEnums.sendgrid.getCode()));
        Update update = new Update();
        update.set("onlineStatus", AccountOnlineStatus.ONLINE.getCode());
        update.set("changeOnlineStatusTime", new Date());
        update.set("loginError", "");
        update.set("isCheck", true);
        mongoTemplate.updateMulti(query, update, Account.class);
    }

    public void updateSetGroup(List<String> ids, String userID, String id) {
        Query query = new Query(Criteria.where("userID").is(userID).and("_id").in(ids));
        Update update = new Update();
        update.set("groupID", id);
        mongoTemplate.updateMulti(query, update, Account.class);
    }

    public List<Account> findbyEmails(List<String> senders) {
        Query query = new Query(Criteria.where("email").in(senders));
        query.fields().include("_id", "email");
        return mongoTemplate.find(query, Account.class);
    }

    public Long countByGroupIdListAndUserID(List<String> ids, String userID) {
        Query query = new Query(Criteria.where("groupID").in(ids).and("userID").is(userID));
        return mongoTemplate.count(query, Account.class);
    }
}
