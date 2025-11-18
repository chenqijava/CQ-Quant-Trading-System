package com.nyy.gmail.cloud.repository.mongo;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.pagination.MongoPageHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.ProxyAccount;
import com.nyy.gmail.cloud.model.dto.ProxyAccountListDTO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProxyAccountRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoPageHelper mongoPageHelper;

    public void saveProxyAccount(ProxyAccount proxyAccount) {
        mongoTemplate.insert(proxyAccount);
    }

    public void updateProxyAccount(ProxyAccount proxyAccount) {
        Query query = new Query(Criteria
                .where("_id").is(proxyAccount.get_id())
                .and("version").is(proxyAccount.getVersion())
        );
        Update update = new Update();
        if (StringUtils.isNotEmpty(proxyAccount.getDesc())) {
            update.set("desc", proxyAccount.getDesc());
        }
        if (StringUtils.isNotEmpty(proxyAccount.getPlatform())) {
            update.set("platform", proxyAccount.getPlatform());
        }
        if (StringUtils.isNotEmpty(proxyAccount.getAccount())) {
            update.set("account", proxyAccount.getAccount());
        }
        if (StringUtils.isNotEmpty(proxyAccount.getProtocol())) {
            update.set("protocol", proxyAccount.getProtocol());
        }
        update.set("maxVpsNum", proxyAccount.getMaxVpsNum());
        update.set("token", proxyAccount.getToken());
        update.set("enable", proxyAccount.getEnable());
        update.inc("version", 1);
        mongoTemplate.updateFirst(query, update, ProxyAccount.class);
    }

    public ProxyAccount findProxyAccountById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        ProxyAccount proxyAccount = mongoTemplate.findOne(query, ProxyAccount.class);
        return proxyAccount;
    }

    public ProxyAccount findProxyAccountByIdUserID(String id, String userID) {
        Query query = new Query(Criteria.where("_id").is(id).and("userID").is(userID));
        ProxyAccount proxyAccount = mongoTemplate.findOne(query, ProxyAccount.class);
        return proxyAccount;
    }

    public List<ProxyAccount> findProxyAccountByStatus(boolean enable) {
        Query query = new Query(Criteria.where("enable").is(enable));
        List<ProxyAccount> proxyAccounts = mongoTemplate.find(query, ProxyAccount.class);
        return proxyAccounts;
    }

    public List<ProxyAccount> findProxyAccountByIds(List<String> ids, String userID) {
        Query query = new Query(Criteria.where("userID").is(userID).and("_id").in(ids));
        List<ProxyAccount> proxyAccountList = mongoTemplate.find(query, ProxyAccount.class);
        return proxyAccountList;
    }

    public void deleteProxyAccountById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, ProxyAccount.class);
    }

    public void deleteProxyAccountByIds(List<String> ids, String userID) {
        Query query = new Query(Criteria.where("userID").is(userID).and("_id").in(ids));
        mongoTemplate.remove(query, ProxyAccount.class);
    }

    public void updateProxyAccountByUserIDEnable(String userID) {
        Query query = new Query(Criteria.where("userID").is(userID));
        Update update = new Update();
        update.set("enable", false);
        mongoTemplate.updateMulti(query, update, ProxyAccount.class);
    }

    public void updateProxyAccountByUserIDIdEnable(String userID, String id, Boolean enable) {
        Criteria criteria = Criteria.where("_id").is(id);
        if (!Session.currentSession().isAdmin()) {
            criteria.and("userID").is(userID);
        }
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("enable", enable);
        mongoTemplate.updateFirst(query, update, ProxyAccount.class);
    }


    public PageResult<ProxyAccount> findProxyAccountPageList(ProxyAccountListDTO proxyAccountListDTO, int pageSize, int pageNumber) {
        if (proxyAccountListDTO == null) {
            Query query = new Query();
            PageResult<ProxyAccount> pageResult = mongoPageHelper.pageQuery(query, ProxyAccount.class, pageSize, pageNumber);
            return pageResult;
        } else {
            Query query = new Query();
            if (StringUtils.isNotEmpty(proxyAccountListDTO.getDesc())) {
                query.addCriteria(Criteria.where("desc").regex(proxyAccountListDTO.getDesc()));
            }
            if (StringUtils.isNotEmpty(proxyAccountListDTO.getPlatform())) {
                query.addCriteria(Criteria.where("platform").regex(proxyAccountListDTO.getPlatform()));
            }
            if (StringUtils.isNotEmpty(proxyAccountListDTO.getToken())) {
                query.addCriteria(Criteria.where("token").regex(proxyAccountListDTO.getToken()));
            }
            if (StringUtils.isNotEmpty(proxyAccountListDTO.getUserID())) {
                query.addCriteria(Criteria.where("userID").is(proxyAccountListDTO.getUserID()));
            }
            PageResult<ProxyAccount> pageResult = mongoPageHelper.pageQuery(query, ProxyAccount.class, pageSize, pageNumber);
            return pageResult;
        }
    }
}
