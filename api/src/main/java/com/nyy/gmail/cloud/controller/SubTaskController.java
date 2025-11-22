package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.model.vo.task.MsgTaskVO;
import com.nyy.gmail.cloud.model.vo.task.SubTaskVO;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.service.SubTaskService;
import com.nyy.gmail.cloud.utils.DateUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/task"})
public class SubTaskController {

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private SubTaskService subTaskService;

    private static List<String> CAN_QUERY_TYPE = List.of(TaskTypesEnums.AccountImport.getCode(), TaskTypesEnums.ImageRecognition.getCode(), TaskTypesEnums.BatchSendEmailTestV2.getCode());

    @PostMapping("/stop")
    public Result stopTask(@RequestBody IdsListDTO data) {
        return ResponseResult.success();
    }

    @PostMapping("/{pageSize}/{pageNo}")
    public Result<PageResult<GroupTask>> list(@PathVariable Integer pageSize, @PathVariable Integer pageNo, @RequestBody(required = false) Map<String, Object> filters) {

        Map<String, Object> params = (Map)filters.get("filters");
        params.put("userID", Session.currentSession().userID);
        if (!params.containsKey("type")) {
            params.put("type", Map.of("$in", CAN_QUERY_TYPE));
        }
        if (params.containsKey("createTime")) {
            Object createTime = params.get("createTime");
            if (createTime instanceof Map) {
                if (((Map<String, Object>) createTime).containsKey("$gte")) {
                    ((Map<String, Object>) createTime).put("$gte",
                            DateUtil.getDateByFormat(((Map<?, ?>) createTime).get("$gte").toString(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                }
                if (((Map<String, Object>) createTime).containsKey("$lte")) {
                    ((Map<String, Object>) createTime).put("$lte",
                            DateUtil.getDateByFormat(((Map<?, ?>) createTime).get("$lte").toString(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                }
            }
        }
        if (params.containsKey("finishTime")) {
            Object finishTime = params.get("finishTime");
            if (finishTime instanceof Map) {
                if (((Map<String, Object>) finishTime).containsKey("$gte")) {
                    ((Map<String, Object>) finishTime).put("$gte",
                            DateUtil.getDateByFormat(((Map<?, ?>) finishTime).get("$gte").toString(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                }
                if (((Map<String, Object>) finishTime).containsKey("$lte")) {
                    ((Map<String, Object>) finishTime).put("$lte",
                            DateUtil.getDateByFormat(((Map<?, ?>) finishTime).get("$lte").toString(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                }
            }
        }
        return ResponseResult.success(subTaskService.list(pageSize,pageNo,params));
    }

    @PostMapping("/processDetail/{taskId}/{pageSize}/{pageNo}")
    public Result<PageResult<SubTask>> queryProcessDetail(@PathVariable Integer pageSize, @PathVariable Integer pageNo, @PathVariable String taskId, @RequestBody(required = false) Map<String, Object> filters) {
        Map<String, Object> params = (Map)filters.get("filters");
        return ResponseResult.success(subTaskService.queryProcessDetail(pageSize,pageNo,taskId,params, Session.currentSession().userID));
    }

    @ResponseBody()
    @PostMapping("exportFailedAccount/{taskId}")
    public void exportFailedAccount(@PathVariable String taskId, HttpServletResponse response) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("status", "failed");
        PageResult<SubTask> result = subTaskService.queryProcessDetail(100000, 1, taskId, params, Session.currentSession().userID);
        List<String> lines = result.getData().stream().map(e -> e.getParams().get("addData").toString()).toList();

        response.setHeader("Cache-Control", "public");

        ServletOutputStream outputStream = response.getOutputStream();
        byte[] buff = String.join("\n", lines).getBytes();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength(buff.length);
        response.setHeader("Content-Disposition", "attachment;filename=" + UriEncoder.encode("account.txt"));
        outputStream.write(buff, 0, buff.length);
        outputStream.flush();
    }
}
