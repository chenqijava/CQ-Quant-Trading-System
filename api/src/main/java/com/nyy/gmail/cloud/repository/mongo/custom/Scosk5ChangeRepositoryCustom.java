package com.nyy.gmail.cloud.repository.mongo.custom;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.entity.mongo.SocksChange;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface Scosk5ChangeRepositoryCustom {
    public void updateByBelongUser(String id, String source, List<String> target);
}

@Slf4j
@Repository
class Scosk5ChangeRepositoryCustomImpl implements Scosk5ChangeRepositoryCustom {
    @Resource
    private MongoTemplate mongoTemplate;

    public void updateByBelongUser(String id, String source, List<String> target) {
        Criteria criteria = Criteria.where("_id").is(id);
        if (!Session.currentSession().isAdmin()) {
            criteria.and("userID").is(Session.currentSession().getUserID());
        }
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("source", source);
        update.set("target", target);
        mongoTemplate.updateMulti(query, update, SocksChange.class);
    }
}