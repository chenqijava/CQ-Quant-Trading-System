package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.utils.TaskUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AccountServiceHelper {


    @Resource
    private TaskUtil taskUtil;



    public void online(IdsListDTO ids, String userID) {
//        taskUtil.createGroupTask(ids.getIds(), TaskTypesEnums.AccountLogin, new HashMap<>(Map.of(
//                "ids",ids.getIds()
//        )), userID);
    }

    public void offline(IdsListDTO ids, String userID) {
//        taskUtil.createGroupTask(ids.getIds(), TaskTypesEnums.AccountLogout, new HashMap<>(Map.of(
//                "ids",ids.getIds()
//        )), userID);
    }


}
