package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.service.AccountPlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CheckAccountPlatformStockJob {

    @Autowired
    private AccountPlatformService accountPlatformService;

    @Autowired
    private UserRepository userRepository;

    @Value("${application.taskType}")
    private String taskType;

    @Async("other")
    @Scheduled(cron = "0 0/5 * * * ?")
    public void run () {
        if (taskType.equals("googleai")) {
            return;
        }
        accountPlatformService.updateStock(Constants.ADMIN_USER_ID);
//        List<User> all = userRepository.findAll();
//        for (User user : all) {
//            if (user.getStatus().equals("enable")) {
//                accountPlatformService.updateStock(user.getUserID());
//            }
//        }
    }

}
