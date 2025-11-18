package com.nyy.gmail.cloud.repository.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.nyy.gmail.cloud.entity.mysql.ApiKey;

import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKey, String>, JpaSpecificationExecutor<ApiKey> {
    ApiKey findByApiKey(String apiKey);

    List<ApiKey> findByUserIDEquals(String userID);
}
