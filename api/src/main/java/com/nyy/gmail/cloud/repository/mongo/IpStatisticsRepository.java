package com.nyy.gmail.cloud.repository.mongo;

import jakarta.annotation.Resource;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.nyy.gmail.cloud.entity.mongo.IpStatistics;

@Repository
public class IpStatisticsRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    public IpStatistics findOne(String userID, String ip){
        Query query = new Query(Criteria.where("userID").is(userID).and("ip").is(ip));
        return mongoTemplate.findOne(query, IpStatistics.class);
    }

    public void saveIpStatistics(IpStatistics ipStatistics){
        mongoTemplate.insert(ipStatistics);
    }

    public void updateIpStatistics(IpStatistics ipStatistics){
        mongoTemplate.save(ipStatistics);
    }
}
