package com.nyy.gmail.cloud.tasks.sieve;

import com.nyy.gmail.cloud.entity.mongo.GroupTask;

public interface SieveActiveTask {
    void publishTask(GroupTask groupTask);
    void runTask(GroupTask groupTask);
    void checkTask(GroupTask groupTask);
    void stopTask(GroupTask groupTask);
    void forceStopTask(GroupTask groupTask);
}
