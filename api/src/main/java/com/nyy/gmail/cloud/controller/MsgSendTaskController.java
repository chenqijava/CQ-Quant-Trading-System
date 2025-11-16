package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.model.dto.task.MsgTaskDTO;
import com.nyy.gmail.cloud.model.dto.task.MsgTaskStopDTO;
import com.nyy.gmail.cloud.model.vo.task.MsgTaskVO;
import com.nyy.gmail.cloud.model.vo.task.SubTaskVO;
import com.nyy.gmail.cloud.service.MsgTaskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
//@RequiredPermission(MenuType.batchSendMessageTask)
@RequestMapping({"/api/consumer/task/"})
public class MsgSendTaskController {

    @Autowired
    private MsgTaskService msgTaskService;

    @Autowired
    private HttpServletRequest request;

    @PostMapping("/{pageSize}/{pageNo}")
    public PageResult<MsgTaskVO> queryTaskList(@PathVariable Integer pageSize, @PathVariable Integer pageNo, @RequestBody(required = false) Map<String, Object> filters) {

        Map<String, Object> params = (Map)filters.get("filters");
        params.put("userId", request.getSession().getAttribute("userID"));
        return msgTaskService.queryMsgTaskByPage(pageSize,pageNo,params);
    }

    @PostMapping("/processDetail/{taskId}/{pageSize}/{pageNo}")
    public PageResult<SubTaskVO> queryProcessDetail(@PathVariable Integer pageSize, @PathVariable Integer pageNo, @PathVariable String taskId, @RequestBody(required = false) Map<String, Object> filters) {
        Map<String, Object> params = (Map)filters.get("filters");
        return msgTaskService.queryProcessDetail(pageSize,pageNo,taskId,params, Session.currentSession().userID);
    }

    @PostMapping("/createTask")
    public Result  addTask(@RequestBody MsgTaskDTO taskDTO) throws IOException {

        taskDTO.setUserId((String)request.getSession().getAttribute("userID"));
        return msgTaskService.crateTask(taskDTO);
    }

    @PostMapping("/finish")
    public Result  stopTask(@RequestBody MsgTaskStopDTO dto){

        return msgTaskService.finishTask(dto.getIds(), Session.currentSession().userID);
    }

    @PostMapping("/delete")
    public Result batchDelete(@RequestBody(required = false) List<String> ids){

        return msgTaskService.batchDelete(ids, Session.currentSession().userID);
    }
}
