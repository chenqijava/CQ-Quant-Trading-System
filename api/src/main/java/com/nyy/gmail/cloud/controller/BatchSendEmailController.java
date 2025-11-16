package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.AccountTypeEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.service.BatchSendEmailService;
import com.nyy.gmail.cloud.utils.DateUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.*;

@Slf4j
@RestController
@RequiredPermission(MenuType.batchSendEmail)
@RequestMapping({"/api/batchSendEmail"})
public class BatchSendEmailController {

    @Autowired
    private BatchSendEmailService batchSendEmailService;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    // 1. 创建任务
    @PostMapping({"/save"})
    public Result save(@RequestBody(required = false) BatchSendEmailReqDto reqDTO) {
        batchSendEmailService.save(reqDTO, Session.currentSession().userID);
        return ResponseResult.success();
    }


    @PostMapping({"/test"})
    public Result test(@RequestBody(required = false) BatchSendEmailTestReqDto reqDTO) {
        GroupTask groupTask = batchSendEmailService.test(reqDTO, Session.currentSession().userID);
        return ResponseResult.success(groupTask.get_id());
    }

    @PostMapping({"/testV2"})
    public Result testV2(@RequestBody(required = false) BatchSendEmailTestReqDto reqDTO) {
//        if (!Session.currentSession().userID.equals(Constants.ADMIN_USER_ID)) {
//            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
//        }

        GroupTask groupTask = batchSendEmailService.testV2(reqDTO, Session.currentSession().userID);
        return ResponseResult.success();
    }

    // 2. 列表
    @PostMapping({"/{pageSize}/{pageNum}"})
    public Result<PageResult<GroupTask>> list(@PathVariable Integer pageSize, @PathVariable Integer pageNum,
                                   @RequestBody(required = false) Params params) {
        if (params.getFilters() == null) {
            params.setFilters(new HashMap<>());
        }
        params.getFilters().put("userID", Session.currentSession().userID);
        params.getFilters().put("type", TaskTypesEnums.BatchSendEmail.getCode());

        PageResult<GroupTask> pageResult = batchSendEmailService.findByPagination(params, pageSize, pageNum);
        pageResult.getData().forEach(e -> {
            e.setIds(null);
        });
        return ResponseResult.success(pageResult);
    }

    // 3. 删除
    @PostMapping({"/delete"})
    public Result delete(@RequestBody(required = false)IdsListDTO data) {
        batchSendEmailService.delete(data);
        return ResponseResult.success();
    }

    // 4. 进度详情列表
    @PostMapping({"/detail/{id}"})
    public Result<BatchSendEmailDetailRespDto> detail(@PathVariable String id) {
        BatchSendEmailDetailRespDto detailRespDto = batchSendEmailService.detail(id);
        return ResponseResult.success(detailRespDto);
    }

    @PostMapping({"/detailList/{id}/{pageSize}/{pageNum}"})
    public Result<PageResult<SubTask>> detailList(@PathVariable String id, @PathVariable Integer pageSize, @PathVariable Integer pageNum, @RequestBody(required = false) Params params) {
        if (params.getFilters() == null) {
            params.setFilters(new HashMap<>());
        }
        params.getFilters().put("userID", Session.currentSession().userID);
        params.getFilters().put("groupTaskId", id);

        PageResult<SubTask> subTaskPageResult = batchSendEmailService.detailList(id, pageSize, pageNum, params);
        return ResponseResult.success(subTaskPageResult);
    }

    @PostMapping({"/executeAB/{id}/{type}"})
    public Result executeAB(@PathVariable String id, @PathVariable String type) {
        batchSendEmailService.executeAB(id, type);
        return ResponseResult.success();
    }

    // 5. 暂停
    @PostMapping({"/pause"})
    public Result pause(@RequestBody(required = false)IdsListDTO data) {
        batchSendEmailService.pause(data);
        return ResponseResult.success();
    }

    @PostMapping({"/start"})
    public Result start(@RequestBody(required = false)IdsListDTO data) {
        batchSendEmailService.start(data);
        return ResponseResult.success();
    }

    // 6. 结束任务
    @PostMapping({"/stop"})
    public Result stop(@RequestBody(required = false)IdsListDTO data) {
        batchSendEmailService.stop(data);
        return ResponseResult.success();
    }

    // 7. 任务检测
    @PostMapping({"/detailTest/{id}"})
    public Result<GroupTask> detailTest(@PathVariable String id) {
        GroupTask groupTask = batchSendEmailService.getDetailTest(id);
        return ResponseResult.success(groupTask == null || !groupTask.getUserID().equals(Session.currentSession().getUserID()) ? null : groupTask);
    }

    @PostMapping({"/test/{id}/{pageSize}/{pageNum}"})
    public Result<PageResult<SubTask>> listTest(@PathVariable String id, @PathVariable Integer pageSize, @PathVariable Integer pageNum, @RequestBody(required = false) Params params) {
        if (params.getFilters() == null) {
            params.setFilters(new HashMap<>());
        }
        params.getFilters().put("userID", Session.currentSession().userID);
//        params.getFilters().put("groupTaskId", id);

        PageResult<SubTask> subTaskPageResult = batchSendEmailService.listTest(id, pageSize, pageNum, params);
        return ResponseResult.success(subTaskPageResult);
    }

    // 8. 二次营销
    @PostMapping({"/secondSend"})
    public Result<List<SecondSendRespDto>> secondSend() {
        List<SecondSendRespDto> secondSend = batchSendEmailService.secondSend();
        return ResponseResult.success(secondSend);
    }


    @PostMapping("export")
    public void export(String groupTaskId, String fields, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userID = Session.currentSession().getUserID();
        Optional<GroupTask> groupTask = groupTaskRepository.findById(groupTaskId);
        List<SubTask> subTaskList = subTaskRepository.findAllByGroupTaskId(groupTaskId);
        List<String> systemEmailList = ((List<String>)groupTask.get().getParams().get("systemEmail"));

        // 1. 发送账号
        // 2. 是否打开
        // 3. 是否点击
        // 4. 是否回复
        List<String> headers = new ArrayList<>();
        if (fields != null && fields.contains("1")) {
            headers.add("发送账号");
        }
        headers.add("接收账号");
        headers.add("发送状态");
        if (fields != null && fields.contains("2")) {
            headers.add("是否打开");
        }
        if (fields != null && fields.contains("3")) {
            headers.add("是否点击");
        }
        if (fields != null && fields.contains("4")) {
            headers.add("是否回复");
        }
        headers.add("结果");
        headers.add("创建时间");
        headers.add("完成时间");

//        String [] headers = {"发送账号", "接收账号", "发送状态", "是否打开", "是否点击", "是否回复", "结果", "创建时间", "完成时间"};
        List<List<String>> data = subTaskList.stream().filter(e -> {
            String email = e.getParams().getOrDefault("email", "").toString();
            if (systemEmailList != null) {
                return !systemEmailList.contains(email);
            }
            return true;
        }).map(e -> {
            List<String> row = new ArrayList<String>();
            if (fields != null && fields.contains("1")) {
                row.add(e.getParams().getOrDefault("sendEmail", "").toString());
            }
            row.add(e.getParams().getOrDefault("email", "").toString());
            row.add(e.getStatus() == null ? "" : e.getStatus().equals("success") ? "成功" : e.getStatus().equals("failed") ? "失败" :
                    e.getParams().getOrDefault("tuiRetry", "").toString().equals("") ? "发送中" : "重试中");
            if (fields != null && fields.contains("2")) {
                row.add(e.getResult() != null && e.getResult().getOrDefault("open", "").toString().equals("1") ? "是" : "否");
            }
            if (fields != null && fields.contains("3")) {
                row.add(e.getResult() != null && e.getResult().getOrDefault("click", "").toString().equals("1") ? "是" : "否");
            }
            if (fields != null && fields.contains("4")) {
                row.add(e.getResult() != null && e.getResult().getOrDefault("reply", "").toString().equals("1") ? "是" :
                        e.getResult() != null && e.getResult().getOrDefault("reply", "").toString().equals("2") ? "退信" : "否");
            }
            row.add(e.getResult() == null ? "" : e.getResult().getOrDefault("msg", "").toString().equals("收到邮件") ? "正常" : e.getResult().getOrDefault("msg", "").toString());
            row.add(e.getCreateTime() != null ? DateUtil.formatByDate(e.getCreateTime(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM) : "");
            row.add(e.getFinishTime() != null ? DateUtil.formatByDate(e.getFinishTime(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM) : "");
            return row;
        }).toList();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // 写表头
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
        }

        // 写数据
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            List<String> rowData = data.get(i);
            for (int j = 0; j < rowData.size(); j++) {
                row.createCell(j).setCellValue(rowData.get(j));
            }
        }

        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        response.setHeader("Cache-Control", "public");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + UriEncoder.encode(groupTask.map(GroupTask::getDesc).orElse("detail") + "_"+ data.size() +".xlsx"));
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        // 输出文件
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            outputStream.flush();
        }
        workbook.close();
    }

    @PostMapping("exportFail")
    public void export(String groupTaskId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userID = Session.currentSession().getUserID();
        Optional<GroupTask> groupTask = groupTaskRepository.findById(groupTaskId);
        List<SubTask> subTaskList = subTaskRepository.findAllByGroupTaskId(groupTaskId);
        List<String> emails = subTaskList.stream().filter(e -> e.getStatus().equals("failed")).map(e -> e.getParams().getOrDefault("email", "").toString()).toList();
        String fileName = URLEncoder.encode(groupTask.map(GroupTask::getDesc).orElse("detail") + "_failed_" +emails.size()+ ".txt", "UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

        // 写入内容
        try (PrintWriter writer = response.getWriter()) {
            emails.forEach(writer::println);
        }
    }

    @PostMapping("systemEmailDetail/{groupTaskId}/{pageSize}/{pageNum}")
    public Result<PageResult<SubTask>> systemEmailDetail(@PathVariable String groupTaskId, @PathVariable int pageSize, @PathVariable int pageNum) {
        GroupTask groupTask = groupTaskRepository.findById(groupTaskId).orElse(null);
        if (groupTask == null) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        String otherEmails = ((Map)groupTask.getParams().get("reqDto")).getOrDefault("otherEmails", "").toString();
        List<String> list = Arrays.stream(otherEmails.split("\n")).toList();
        List<String> systemEmailList = ((List<String>)groupTask.getParams().get("systemEmail"));
        Params params = new Params();
        if (params.getFilters() == null) {
            params.setFilters(new HashMap<>());
        }
        List<String> emails = new ArrayList<>();
        if (systemEmailList != null) {
            for (String email : systemEmailList) {
                if (!list.contains(email)) {
                    emails.add(email);
                }
            }
        }
        params.getFilters().put("userID", Session.currentSession().userID);
        params.getFilters().put("groupTaskId", groupTaskId);
        params.getFilters().put("params.email", Map.of("$in", emails));

        PageResult<SubTask> subTaskPageResult = batchSendEmailService.detailList(groupTaskId, pageSize, pageNum, params);
        return ResponseResult.success(subTaskPageResult);
    }

    @PostMapping("otherEmailDetail/{groupTaskId}/{pageSize}/{pageNum}")
    public Result<PageResult<SubTask>> otherEmailDetail(@PathVariable String groupTaskId, @PathVariable int pageSize, @PathVariable int pageNum) {
        GroupTask groupTask = groupTaskRepository.findById(groupTaskId).orElse(null);
        if (groupTask == null) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        String otherEmails = ((Map)groupTask.getParams().get("reqDto")).getOrDefault("otherEmails", "").toString();
        List<String> list = Arrays.stream(otherEmails.split("\n")).toList();
        Params params = new Params();
        if (params.getFilters() == null) {
            params.setFilters(new HashMap<>());
        }
        params.getFilters().put("userID", Session.currentSession().userID);
        params.getFilters().put("groupTaskId", groupTaskId);
        params.getFilters().put("params.email", Map.of("$in", list));

        PageResult<SubTask> subTaskPageResult = batchSendEmailService.detailList(groupTaskId, pageSize, pageNum, params);
        return ResponseResult.success(subTaskPageResult);
    }
}
