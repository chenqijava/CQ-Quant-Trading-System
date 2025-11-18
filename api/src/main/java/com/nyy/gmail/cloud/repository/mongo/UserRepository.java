package com.nyy.gmail.cloud.repository.mongo;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.nyy.gmail.cloud.common.pagination.MongoPageHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Role;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.enums.UserStatusEnum;
import com.nyy.gmail.cloud.model.dto.UserApiTokenSaveDTO;
import com.nyy.gmail.cloud.model.dto.UserIDDTO;
import com.nyy.gmail.cloud.model.dto.UserListDTO;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Repository
public class UserRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoPageHelper mongoPageHelper;

    public void saveUser(User user) {
        mongoTemplate.insert(user);
    }

    public void updateUser(User user) {
        mongoTemplate.save(user);
    }

    public User findOneAndUpdateByPassword(String userID, String password, String sessionId) {
        Query query = new Query(Criteria.where("userID").is(userID).and("password").is(password));
        Update update = new Update();
        update.set("lastLoginTime", new Date());
        update.set("session", sessionId);
//        User user = mongoTemplate.findAndModify(query, update, User.class);
//        Role role = mongoTemplate.findOne(new Query(Criteria.where("_id").is(user.getRole())), Role.class);
//        user.setRole(role.getName());
        return mongoTemplate.findAndModify(query, update, User.class);
    }

    public User findOneAndUpdateBySession(String userID, String sessionId) {
        Query query = new Query(Criteria.where("userID").is(userID)
                .and("session").is(sessionId)
                .and("status").is("enable"));
        Update update = new Update();
        update.set("lastLoginTime", new Date());
        return mongoTemplate.findAndModify(query, update, User.class);
    }

    public User findOneAndUpdateByBalance(String id, Long chargeValue) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().inc("balance", chargeValue);
        return mongoTemplate.findAndModify(query, update, User.class);
    }

    public User findOneAndUpdateByBalanceV2(String id, Long finalValue) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().set("balance", finalValue);
        return mongoTemplate.findAndModify(query, update, User.class);
    }

   public User findOneAndUpdateByBalance_(String userID, BigDecimal chargeValue) {
        Query query = new Query(Criteria.where("userID").is(userID));
        Update update = new Update().set("balance", chargeValue);
        return mongoTemplate.findAndModify(query, update, User.class);
    }

    public User findOneByUserID(String userID) {
        Query query = new Query(Criteria.where("userID").is(userID).and("status").is("enable"));
        return mongoTemplate.findOne(query, User.class);
    }

    public User findOneByID(String id) {
        Query query = new Query(Criteria.where("_id").is(id).and("status").is("enable"));
        return mongoTemplate.findOne(query, User.class);
    }

    public User findOneByUserID_(String userID) {
        Query query = new Query(Criteria.where("userID").is(userID));
        return mongoTemplate.findOne(query, User.class);
    }

    public User findOneByPassword(String userID, String password) {
        Query query = new Query(Criteria.where("userID").is(userID).and("password").is(password));
        return mongoTemplate.findOne(query, User.class);
    }

    public User findOneAndUpdateByNewPassword(String userID, String password, String session, String newPassword) {
        Query query = new Query(Criteria.where("userID").is(userID).and("password").is(password).and("status").is("enable"));
        Update update = new Update();
        update.set("session", session);
        update.set("password", newPassword);
        return mongoTemplate.findAndModify(query, update, User.class);
    }

    public User findOneAndUpdateByNewName(String userID, String name, String newName) {
        Query query = new Query(Criteria.where("userID").is(userID).and("name").is(name).and("status").is("enable"));
        Update update = new Update();
        update.set("name", newName);
        return mongoTemplate.findAndModify(query, update, User.class);
    }

    public User findOneAndUpdateBySave(String userID, String password, String session, String name, String socks5Area, String role, String botId, String token, String userApiKey) {
        Query query = new Query(Criteria.where("userID").is(userID));
        Update update = new Update();
        if (StringUtils.isNotEmpty(password)) {
            update.set("password", password);
        }
        if (StringUtils.isNotEmpty(role)) {
            update.set("role", role);
        }
        if (StringUtils.isNotEmpty(session)) {
            update.set("session", session);
        }
        update.set("name", name);
        update.set("socks5Area", socks5Area);
        update.set("botId", botId);
        update.set("token", token);
        if (StringUtils.isNotBlank(userApiKey)) {
            update.set("userApiKey", userApiKey);
        }
//        update.set("conversionFactor", conversionFactor);
        return mongoTemplate.findAndModify(query, update, User.class);
    }

    /**
     * 更新汇率的转换因子
     *
     * @param userID
     * @param conversionFactor
     * @return
     */
    public User findOneAndUpdateBySave(String userID, String conversionFactor) {
        Query query = new Query(Criteria.where("userID").is(userID));
        Update update = new Update();
        update.set("conversionFactor", conversionFactor);
        return mongoTemplate.findAndModify(query, update, User.class);
    }

    public Long countByRole(Role role) {
        Query query = new Query(Criteria.where("role").is(role.get_id()));
        return mongoTemplate.count(query, User.class);
    }

    public User findOneById(String id) {
        Query query = new Query(new Criteria().andOperator(Criteria.where("_id").is(id)));
        User user = mongoTemplate.findOne(query, User.class);
        return user;
    }

    public User findOneByIdUserID(String id, String userID) {
        Query query = new Query(new Criteria().andOperator(Criteria.where("_id").is(id), new Criteria().orOperator(Criteria.where("userID").is(userID), Criteria.where("createUserID").is(userID))));
        User user = mongoTemplate.findOne(query, User.class);
        return user;
    }

    public void findAndUpdateByIdsUserID(List<String> ids, String userID, String password, String session) {
        Criteria criteria =
                new Criteria().andOperator(Criteria.where("_id").in(ids));

//        admin可以修改任意账号密码
        if (!"admin".equals(userID)) {
            criteria.andOperator(new Criteria().orOperator(Criteria.where("userID").is(userID),
                    Criteria.where("createUserID").is(userID)));
        }
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("password", password);
        update.set("session", session);
        UpdateResult result = mongoTemplate.updateMulti(query, update, User.class);
    }

    public List<User> findUserList(UserListDTO userListDTO) {
        if (userListDTO == null) {
            Query query = new Query();
            List<User> userList = mongoTemplate.find(query, User.class);
            return userList;
        } else {
            Query query = new Query();
            if (StringUtils.isNotEmpty(userListDTO.getCreateUserID())) {
                query.addCriteria(Criteria.where("createUserID").is(userListDTO.getCreateUserID()));
            }
            if (StringUtils.isNotEmpty(userListDTO.getName())) {
                query.addCriteria(Criteria.where("name").regex(userListDTO.getName()));
            }
            if (StringUtils.isNotEmpty(userListDTO.getUserID())) {
                query.addCriteria(Criteria.where("userID").is(userListDTO.getUserID()));
            }
            List<User> userList = mongoTemplate.find(query, User.class);
            return userList;
        }
    }

    public List<User> findUserPageList(UserListDTO userListDTO, int pageSize, int pageNumber) {
        if (userListDTO == null) {
            Query query = new Query();
//            PageResult<User> pageResult = mongoPageHelper.pageQuery(query, User.class, pageSize, pageNumber);
//            return pageResult;
            List<User> result = mongoTemplate.find(query, User.class);
            for (User user : result) {
                user.setRole(mongoTemplate.findOne(new Query(Criteria.where("_id").is(user.getRole())), Role.class).getName());
            }
            return result;
        } else {
            Query query = new Query();
            query.limit(pageSize);
            query.skip(pageSize * (pageNumber - 1));
            if (StringUtils.isNotEmpty(userListDTO.getCreateUserID())) {
                query.addCriteria(Criteria.where("createUserID").is(userListDTO.getCreateUserID()));
            }
            if (StringUtils.isNotEmpty(userListDTO.getName())) {
                query.addCriteria(Criteria.where("name").regex(userListDTO.getName()));
            }
            if (StringUtils.isNotEmpty(userListDTO.getUserID())) {
                query.addCriteria(Criteria.where("userID").is(userListDTO.getUserID()));
            }
//            PageResult<User> pageResult = mongoPageHelper.pageQuery(query, User.class, pageSize, pageNumber);

            List<User> result = mongoTemplate.find(query, User.class);
            for (User user : result) {
                user.setRole(mongoTemplate.findOne(new Query(Criteria.where("_id").is(user.getRole())), Role.class).getName());
            }
            return result;
        }
    }

    // 1 * userID = User(查询)
    //修改customer
    public void updateCustomerByUserID(String userID, List<String> customerIDs) {
        Query query = new Query(Criteria.where("userID").is(userID));
        Update update = new Update();
        update.set("customer", customerIDs);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    // 1 * userID = User(查询)
    //添加(push)customer
    public void pushCustomerByUserID(String userID, String customerId) {
        Query query = new Query(Criteria.where("userID").is(userID));
        Update update = new Update();
        update.push("customer", customerId);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    // 1 * createUserID(查询)
    public List<User> findUserByCreateUserID(String createUserID) {
        Query query = new Query(Criteria.where("createUserID").is(createUserID));
        return mongoTemplate.find(query, User.class);
    }

    public List<User> findUserByCreateUserIDAndUserID(String createUserID, String userID) {
        Query query = new Query(Criteria.where("createUserID").is(createUserID).and("userID").is(userID));
        return mongoTemplate.find(query, User.class);
    }


    public void deleteArrayElement(String userID, String element) {
        Query query = new Query(Criteria.where("userID").is(userID));
        Update update = new Update();
        update.pull("customer", element);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    public List<User> findByUserIDs(List<String> userIDs) {
        Query query = new Query(Criteria.where("userID").in(userIDs));
        return mongoTemplate.find(query, User.class);
    }

    //更新账号登录信息
    public User updateLogin(String userID, String _id, Date lastLoginTime, String session) {
        Criteria criteria = Criteria.where("_id").is(_id);
        //如果是admin用户便不用再检测createUserID
        if (!userID.equals("admin"))
            criteria.and("createUserID").is(userID);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("lastLoginTime", lastLoginTime);
        if (session != null)
            update.set("session", session);
        return mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), User.class);
    }

    //删除user
    public void dropUser(String userID, List<String> users) {
        Criteria criteria = Criteria.where("_id").in(users);
        if (!userID.equals("admin"))
            criteria.and("createUserID").is(userID);
        mongoTemplate.remove(new Query(criteria), User.class);
    }

    public List<User> findByRole(Role role) {
        Query query = new Query(Criteria.where("role").is(role.get_id()));
        return mongoTemplate.find(query, User.class);
    }

    public PageResult<User> findUserByPagination(UserListDTO userListDTO, int pageSize, int page) {
        Query query = new Query();
        String name = userListDTO.getName();
        if (name != null) {
            Pattern pattern = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("name").regex(pattern));
        }

        String userID = userListDTO.getUserID();
        if (userID != null) {
            Pattern pattern = Pattern.compile("^.*" + userID + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("userID").regex(pattern));
        }

        String createUserID = userListDTO.getCreateUserID();
        if (createUserID != null) {
            Pattern pattern = Pattern.compile("^.*" + createUserID + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("createUserID").regex(pattern));
        }

        if (userListDTO.getOpenApi() != null) {
            query.addCriteria(Criteria.where("openApi").is(userListDTO.getOpenApi()));
        }

        if (userListDTO.getFilters() != null) {
          Set<String> excludeKeys = new HashSet<>() {{
              add("name");
              add("userID");
              add("createUserID");
              add("openApi");
          }};
          userListDTO.getFilters().forEach((key, value) -> {
              if (!excludeKeys.contains(key)) {
                  switch (key){
                      default:
                          query.addCriteria(Criteria.where(key).is(value));
                          break;
                  }

              }
          });
        }

        PageResult<User> pageResult = mongoPageHelper.pageQuery(query, User.class, pageSize, page);
        return pageResult;
    }

    public List<User> findUserByRole(String role) {
        Query query = new Query(Criteria.where("role").is(role));
        return mongoTemplate.find(query, User.class);
    }

    public User findUserByToken(String token) {
        Query query = new Query(Criteria.where("token").is(token));
        return mongoTemplate.findOne(query, User.class);

    }

    public UpdateResult allowance(List<String> ids, String userID) {
        Query query = new Query(Criteria.where("_id").in(ids));
        Update update = new Update();
        update.set("status", "enable");
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, User.class);
        return updateResult;

    }

    public UpdateResult forbidden(List<String> ids, String userID) {
        Query query = new Query(Criteria.where("_id").in(ids));
        Update update = new Update();
        update.set("status", "disable");
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, User.class);
        return updateResult;
    }

    public List<String> getAllNoBalanceUser() {
        Query query = new Query();
        query.addCriteria(Criteria.where("balance").is(0));
        query.addCriteria(Criteria.where("status").is("enable"));
        query.addCriteria(Criteria.where("userID").ne("admin"));
        query.fields().include("_id");
        List<String> userIds = mongoTemplate.find(query, User.class).stream().map(user -> user.get_id()).collect(Collectors.toList());
        return userIds;
    }

    public UpdateResult bannedNoBalanceUserByIds(List<String> ids) {
        Query query = new Query();
        query.addCriteria(Criteria.where("balance").is(0));
        query.addCriteria(Criteria.where("_id").in(ids));
        query.addCriteria(Criteria.where("status").is("enable"));
        query.addCriteria(Criteria.where("userID").ne("admin"));
        Update update = new Update();
        update.set("status", "disable");
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, User.class);
        return updateResult;
    }

    public User addUserLoginFailedTime(String userID) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userID").is(userID));
        Update update = new Update();
        update.inc("loginFailedTime", 1);

        User newUser = mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), User.class);
        return newUser;
    }

    public UpdateResult setUserLoginFailed(String userID, Integer loginFailedTime) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userID").is(userID));
        Update update = new Update();
        update.set("loginFailedTime", loginFailedTime);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, User.class);
        return updateResult;
    }

    public User findUserByUserIDApiToken(String userID, String apiToken) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userID").is(userID));
        query.addCriteria(Criteria.where("apiToken").is(apiToken));

        return mongoTemplate.findOne(query, User.class);

    }

    public UpdateResult closeApi(List<String> ids, String userID) {
        Query query = new Query(Criteria.where("_id").in(ids));
        Update update = new Update();
        update.set("openApi", false);
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, User.class);
        return updateResult;
    }

    public long openApi(List<String> ids, String userID) {
        Query query = new Query(Criteria.where("_id").in(ids));
        List<User> users = mongoTemplate.find(query, User.class);
        AtomicLong count = new AtomicLong();
        users.stream().forEach(user -> {
            if (!user.isOpenApi()) {
                Query updateQuery = new Query(Criteria.where("_id").is(user.get_id()));
                Update update = new Update();
                update.set("openApi", true);
                if (user.getApiToken() == null) {
                    update.set("apiToken", UUID.randomUUID().toString());
                }
                UpdateResult updateResult = mongoTemplate.updateFirst(updateQuery, update, User.class);
                count.addAndGet(updateResult.getModifiedCount());
            }
        });

        return count.get();
    }


    public UpdateResult update(List<String> ids, String userID, String key, Object value) {
        Query query = new Query(Criteria.where("_id").in(ids));
        Update update = new Update();
        update.set(key, value);
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, User.class);
        return updateResult;
    }

    public long saveApiIps(UserApiTokenSaveDTO userApiTokenSaveDTO, String userID) {
        Query query = new Query(Criteria.where("_id").in(userApiTokenSaveDTO.getIds()));
        Update update = new Update();
        update.set("ips", userApiTokenSaveDTO.getIps());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, User.class);
        return updateResult.getModifiedCount();
    }

    public long refreshApiToken(UserApiTokenSaveDTO userApiTokenSaveDTO, String userID) {
        Query query = new Query(Criteria.where("_id").in(userApiTokenSaveDTO.getIds()));
        Update update = new Update();
        update.set("apiToken", UUID.randomUUID().toString());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, User.class);
        return updateResult.getModifiedCount();
    }

    public List<UserIDDTO> findUserIDByIds(List<String> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        query.fields().include("userID");
        List<UserIDDTO> users = mongoTemplate.find(query, UserIDDTO.class, "user");
        return users;
    }

    /**
     * 根据创建者ID查询用户
     *
     * @param adminUserId
     * @return
     */
    public List<User> findUserIDsByCreateUserID(String adminUserId) {
        Query query = new Query(Criteria.where("createUserID").is(adminUserId).and("status").is("enable"));
        query.fields().include("userID");
        // 只返回userID
        List<User> users = mongoTemplate.find(query, User.class);
        return users;
    }

    /**
     * 只能改一次
     * @param id
     * @param balance
     * @return
     */
    public boolean updateUserNewBalance(String id, Long balance) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("balance", balance);
        update.set("changedBalance", true);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, User.class);
        return updateResult.getModifiedCount() > 0;
    }

    /**
     * 批量冻结用户
     *
     * @param idList
     * @param userID
     */
    public boolean disabledUser(List<String> idList, String userID) {
        Query query = null;
        if (userID.equals("admin")) {
            query = new Query(Criteria.where("_id").in(idList));
        } else {
            query = new Query(Criteria.where("_id").in(idList).and("createUserID").is(userID));
        }
        Update update = new Update();
        update.set("status", UserStatusEnum.DISABLED.getCode());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, User.class);
        return updateResult.getModifiedCount() > 0;
    }

    /**
     * 批量解冻用户
     *
     * @param idList
     * @param userID
     */
    public boolean enableUser(List<String> idList, String userID) {
        Query query = null;
        if (userID.equals("admin")) {
            query = new Query(Criteria.where("_id").in(idList));
        } else {
            query = new Query(Criteria.where("_id").in(idList).and("createUserID").is(userID));
        }
        Update update = new Update();
        update.set("status", UserStatusEnum.ENABLE.getCode());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, User.class);
        return updateResult.getModifiedCount() > 0;
    }

    /**
     * 批量删除用户
     *
     * @param idList
     * @param userID
     */
    public boolean deleteUser(List<String> idList, String userID) {
        Query query = null;
        if (userID.equals("admin")) {
            query = new Query(Criteria.where("_id").in(idList));
        } else {
            query = new Query(Criteria.where("_id").in(idList).and("createUserID").is(userID));
        }
        DeleteResult remove = mongoTemplate.remove(query, User.class);
        return remove.getDeletedCount() > 0;
    }


    public List<User> findByIds(List<String> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        List<User> users = mongoTemplate.find(query, User.class);
        return users;
    }

    public void deleteByUserID(String userID) {
        Query query = new Query(Criteria.where("userID").is(userID));
        mongoTemplate.remove(query, User.class);
    }

    public List<User> findUserByCreateUserIDs(List<String> userIds) {
        Query query = new Query(Criteria.where("createUserID").in(userIds));
        List<User> users = mongoTemplate.find(query, User.class);
        return users;
    }

    public List<User> findAll() {
        Query query = new Query();
        List<User> users = mongoTemplate.find(query, User.class);
        return users;
    }

    public User findUserByUserApiKey(String apiToken) {
        Query query = new Query(Criteria.where("userApiKey").is(apiToken));
        User user = mongoTemplate.findOne(query, User.class);
        if (user != null && user.getStatus().equals(UserStatusEnum.ENABLE.getCode())) {
            return user;
        } else {
            return null;
        }
    }

    public void updateOpenRecharge(List<String> ids, String open) {
        Query query = new Query(Criteria.where("_id").in(ids));
        Update update = new Update();
        update.set("openRecharge", open);
        mongoTemplate.updateMulti(query, update, User.class);
    }

    public List<User> findByRefererLike(String id) {
        Query query = new Query(Criteria.where("referrer").regex(id));
        return mongoTemplate.find(query, User.class);
    }
}
