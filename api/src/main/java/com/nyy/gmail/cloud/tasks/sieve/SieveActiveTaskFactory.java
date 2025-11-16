package com.nyy.gmail.cloud.tasks.sieve;

import cn.hutool.core.util.StrUtil;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.utils.BeanUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SieveActiveTaskFactory {
    public static SieveActiveTaskFactory instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public SieveActiveTask getTaskBean(GroupTask groupTask) {
        return getTaskBean((String) groupTask.getParams().get("project"), groupTask.getType());
    }

    public SieveActiveTask getTaskBean(String project, String type) {
        String beanName = StrUtil.lowerFirst(project) + StrUtil.upperFirst(type) + "Task";
        return tryGetBean(beanName);
    }

    private SieveActiveTask tryGetBean(String beanName) {
        try {
            Object candidate = BeanUtils.getBean(beanName);
            if (candidate instanceof SieveActiveTask) {
                return (SieveActiveTask) candidate;
            } else {
                log.warn("Bean '{}' is not an instance of SieveActiveTask.", beanName);
            }
        } catch (Exception e) {
            log.info("Bean '{}' does not exist or cannot be loaded: {}", beanName, e.getMessage());
        }
        return null;
    }
}
