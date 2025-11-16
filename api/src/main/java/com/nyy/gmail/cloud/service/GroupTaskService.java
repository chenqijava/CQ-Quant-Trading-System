package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GroupTaskService {


    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private RedissonClient redissonClient;


    public GroupTask findById(String groupTaskId) {
        // 加缓存
        String key = "GroupTaskCache:" + groupTaskId;
        RBucket<GroupTask> bucket = redissonClient.getBucket(key);
        GroupTask groupTask = bucket.get();

        if (groupTask == null) {
            groupTask = groupTaskRepository.findById(groupTaskId).orElse(null);
            long expireTime = 30;
            bucket.set(groupTask, expireTime, TimeUnit.SECONDS);
        }
        return groupTask;
    }
}
