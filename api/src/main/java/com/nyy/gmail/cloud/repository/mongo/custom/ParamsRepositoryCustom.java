package com.nyy.gmail.cloud.repository.mongo.custom;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.nyy.gmail.cloud.entity.mongo.Params;

public interface ParamsRepositoryCustom {
    public void findOneAndUpdateByValue(String code, Object value, String userID);
}

@Slf4j
@Repository
class ParamsRepositoryCustomImpl implements ParamsRepositoryCustom {
    @Resource
    private MongoTemplate mongoTemplate;


    @Override
    public void findOneAndUpdateByValue(String code, Object value, String userID) {
        Query query;
        if(userID == null || userID.equals("")) {
            query = new Query(Criteria.where("code").is(code));
        }
        else {
            query = new Query(Criteria.where("code").is(code).and("userID").is(userID));
        }
        Update update = new Update();
        update.set("value", value);
        mongoTemplate.upsert(query, update, Params.class);
    }
}