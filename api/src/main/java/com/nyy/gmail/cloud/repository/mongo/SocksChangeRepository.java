package com.nyy.gmail.cloud.repository.mongo;

import com.nyy.gmail.cloud.entity.mongo.SocksChange;
import com.nyy.gmail.cloud.repository.mongo.custom.Scosk5ChangeRepositoryCustom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocksChangeRepository extends MongoRepository<SocksChange, String>, Scosk5ChangeRepositoryCustom {
}
