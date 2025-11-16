package com.nyy.gmail.cloud.tasks;

import com.nyy.gmail.cloud.entity.mongo.GroupTask;

public interface BaseTask extends Task {

    boolean publishTask(GroupTask groupTask);

}
