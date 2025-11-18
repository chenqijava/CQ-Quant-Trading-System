package com.nyy.gmail.cloud.repository.mongo;

import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.repository.mongo.custom.GroupTaskRepositoryCustom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupTaskRepository extends MongoRepository<GroupTask, String>, GroupTaskRepositoryCustom {
    List<GroupTask> findAllByTypeAndStatus(String type, String status);

}

