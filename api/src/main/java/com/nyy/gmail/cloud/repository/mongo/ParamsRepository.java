package com.nyy.gmail.cloud.repository.mongo;

import com.nyy.gmail.cloud.entity.mongo.Params;
import com.nyy.gmail.cloud.repository.mongo.custom.ParamsRepositoryCustom;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParamsRepository extends MongoRepository<Params, String>, ParamsRepositoryCustom {
}
