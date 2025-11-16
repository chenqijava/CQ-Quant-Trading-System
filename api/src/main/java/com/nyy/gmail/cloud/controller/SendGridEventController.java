package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.entity.mongo.SendGridEvent;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.repository.mongo.SendGridEventRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sendgrid/events")
@Slf4j
public class SendGridEventController {

    @Autowired
    private SendGridEventRepository sendGridEventRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @NoLogin
    @PostMapping
    public void handleEvents(@RequestBody List<SendGridEvent> events) {
        for (SendGridEvent event : events) {
            log.info("Received SendGrid event: {}", event);

            sendGridEventRepository.save(event);
            if (StringUtils.isNotEmpty(event.getUuid())) {
                if (event.getEvent().equalsIgnoreCase("bounce")) {
                    List<SubTask> subTasks = subTaskRepository.findBySendGridTask(event.getUuid());

                    for (SubTask subTask : subTasks) {
                        subTask.getResult().put("reply", "2");
                        subTask.getResult().put("msg", "SendGrid硬退信("+event.getReason()+")");
                        subTaskRepository.save(subTask);
                    }
                }

                if (event.getEvent().equalsIgnoreCase("blocked")) {
                    List<SubTask> subTasks = subTaskRepository.findBySendGridTask(event.getUuid());

                    for (SubTask subTask : subTasks) {
                        subTask.getResult().put("reply", "2");
                        subTask.getResult().put("msg", "SendGrid被目标服务器拦截("+event.getReason()+")");
                        subTaskRepository.save(subTask);
                    }
                }

                if (event.getEvent().equalsIgnoreCase("dropped")) {
                    List<SubTask> subTasks = subTaskRepository.findBySendGridTask(event.getUuid());

                    for (SubTask subTask : subTasks) {
                        subTask.getResult().put("reply", "2");
                        subTask.getResult().put("msg", "SendGrid未投递("+event.getReason()+")");
                        subTaskRepository.save(subTask);
                    }
                }
            }
        }
    }
}
