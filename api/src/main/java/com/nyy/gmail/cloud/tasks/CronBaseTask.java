package com.nyy.gmail.cloud.tasks;

import com.nyy.gmail.cloud.entity.mongo.Account;

public interface CronBaseTask extends Task {

    boolean cronAddTask(Account account);

}
