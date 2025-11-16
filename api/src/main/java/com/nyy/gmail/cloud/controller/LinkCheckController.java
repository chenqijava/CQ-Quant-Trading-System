package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.model.dto.LinkCheckDetail;
import com.nyy.gmail.cloud.model.dto.LinkCheckReqDto;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.service.LinkCheckService;
import com.nyy.gmail.cloud.utils.DateUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.util.*;

@Slf4j
@RestController
@RequiredPermission(MenuType.linkCheck)
@RequestMapping({"/api/linkCheck"})
public class LinkCheckController {

    @Autowired
    private LinkCheckService linkCheckService;

    @Autowired
    private GroupTaskRepository  groupTaskRepository;

    // 1. 新增
    @PostMapping({"/save"})
    public Result save(@RequestBody(required = false) LinkCheckReqDto reqDTO) {
        linkCheckService.save(reqDTO, Session.currentSession().userID);
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
        params.getFilters().put("type", TaskTypesEnums.EmailLinkCheck.getCode());

        PageResult<GroupTask> pageResult = linkCheckService.findByPagination(params, pageSize, pageNum);
        pageResult.getData().forEach(e -> {
            e.setIds(null);
        });
        return ResponseResult.success(pageResult);
    }

    // 3. 删除
    @PostMapping({"/delete"})
    public Result delete(@RequestBody(required = false) IdsListDTO data) {
        linkCheckService.delete(data);
        return ResponseResult.success();
    }

    // 5. 结束任务
    @PostMapping({"/stop"})
    public Result stop(@RequestBody(required = false)IdsListDTO data) {
        linkCheckService.stop(data);
        return ResponseResult.success();
    }
    // 6. 进度
    @PostMapping({"/detail/{groupTaskId}/{pageSize}/{pageNum}"})
    public Result<PageResult<LinkCheckDetail>> detail(@PathVariable String groupTaskId, @PathVariable Integer pageSize, @PathVariable Integer pageNum, @RequestBody(required = false) Map sorts) {
        PageResult<LinkCheckDetail> pageResult = linkCheckService.detail(sorts, groupTaskId, pageSize, pageNum);
        return ResponseResult.success(pageResult);
    }

    // 7. 下载报告
    @PostMapping("export")
    public void export(String groupTaskId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<GroupTask> groupTask = groupTaskRepository.findById(groupTaskId);
        PageResult<LinkCheckDetail> pageResult = linkCheckService.detail(Map.of("rate", -1), groupTaskId, 100000, 1);

        List<String> headers = new ArrayList<>();
        headers.add("链接");
        headers.add("垃圾率");
        headers.add("发送数量");
        headers.add("状态");
        headers.add("创建时间");
        headers.add("完成时间");

//        String [] headers = {"发送账号", "接收账号", "发送状态", "是否打开", "是否点击", "是否回复", "结果", "创建时间", "完成时间"};
        List<List<String>> data = pageResult.getData().stream().map(e -> {
            List<String> row = new ArrayList<String>();
            row.add(e.getLink());
            row.add(e.getRate().toPlainString());
            row.add(e.getJunkNum() + e.getNormalNum() + "");
            row.add(e.getStatus() == null ? "完成" : "进行中");
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

}
