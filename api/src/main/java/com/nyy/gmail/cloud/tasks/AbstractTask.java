package com.nyy.gmail.cloud.tasks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.SubTaskStatusEnums;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.utils.TaskUtil;

import java.util.Date;
import java.util.Map;

public abstract class AbstractTask implements Task {

    protected abstract SubTaskRepository getSubTaskRepository ();

    protected abstract TaskUtil getTaskUtil () ;

    public SubTask reportTaskStatus(SubTask wt, SubTaskStatusEnums status, Object result) {
        for (int i = 0; i < 10; i++) {
            try {
                if (wt.getResult() != null && wt.getResult().size() > 1) {
                    wt.setStatus(SubTaskStatusEnums.success.getCode());
                    getSubTaskRepository().save(wt);
                    break;
                }
                wt.setStatus(status.getCode());
                if (result != null) {
                    if (result instanceof String) {
                        wt.setResult(Map.of("msg", result.toString()));
                    } else {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> map = mapper.convertValue(result, new TypeReference<Map<String, Object>>() {
                        });
                        wt.setResult(map);
                    }
                }
                wt.setFinishTime(new Date());
                getSubTaskRepository().save(wt);
                getTaskUtil().submitGroupByStatus(wt.getGroupTaskId(), wt.getStatus());
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                wt = getSubTaskRepository().findById(wt.get_id());
            }
        }
        return wt;
    }
}
