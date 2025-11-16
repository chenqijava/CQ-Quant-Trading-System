package com.nyy.gmail.cloud.controller;


import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.JpaPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.MailTemplateStatusEnums;
import com.nyy.gmail.cloud.enums.MailTemplateTypeEnums;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.repository.mongo.MailTemplateGroupRepository;
import com.nyy.gmail.cloud.service.MailTemplateService;
import com.nyy.gmail.cloud.service.UserService;
import com.nyy.gmail.cloud.utils.DateUtil;
import jakarta.annotation.Resource;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.xbill.DNS.dnssec.R;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.nyy.gmail.cloud.common.response.ResultCode.MAIL_TEMPLATE_GROUP_IN_USE;
import static com.nyy.gmail.cloud.common.response.ResultCode.NO_AUTHORITY;

@Slf4j
@RestController
@RequestMapping({"/api/mailTemplate"})
public class MailTemplateController {

    @Autowired
    private MailTemplateService mailTemplateService;

    @Autowired
    private UserService userService;

    @Resource
    private JpaPaginationHelper jpaPaginationHelper;

    @Autowired
    private MailTemplateGroupRepository mailTemplateGroupRepository;

    @PostMapping("{pageSize}/{page}")
    public Result<PageResult<MailTemplate>> list(@PathVariable("pageSize") int pageSize,
                                                 @PathVariable("page") int page,
                                                 @RequestBody(required = false) MailTemplateListDTO mailTemplateListDTO) {
        String userID = Session.currentSession().getUserID();
        if(mailTemplateListDTO.getFilters() == null) {
            mailTemplateListDTO.setFilters(new HashMap<>());
        }
        if (mailTemplateListDTO.getFilters().containsKey("type") && mailTemplateListDTO.getFilters().get("type").equals(
                MailTemplateTypeEnums.USER.getCode()
        )) {
            mailTemplateListDTO.getFilters().put("userID", userID);
        }
        PageResult<MailTemplate> pageResult = mailTemplateService.findByPagination(mailTemplateListDTO, pageSize, page);
        Result<PageResult<MailTemplate>> result = ResponseResult.success(pageResult);
        List<String> ids = result.getData().getData().stream().map(MailTemplate::getGroupID).filter(StringUtils::isNotEmpty).toList();
        List<MailTemplateGroup> groups = mailTemplateGroupRepository.findByIdsIn(ids);
        Map<String, String> map = groups.stream().collect(Collectors.toMap(MailTemplateGroup::get_id, MailTemplateGroup::getGroupName));

        result.getData().getData().forEach(e -> {
            if (StringUtils.isNotEmpty(e.getGroupID()) && map.containsKey(e.getGroupID())) {
                e.setGroupName(map.get(e.getGroupID()));
            }
        });
        return result;
    }

    // 1. 导入
    @PostMapping("import")
    public Result importTemplate(@RequestBody(required = false) ImportMailTemplateReqDto reqDto) {
        String userID = Session.currentSession().getUserID();
        if (reqDto.getTemplateType().equals(MailTemplateTypeEnums.SYSTEM.getCode()) && !userService.isAdmin(userID)) {
            return ResponseResult.failure(NO_AUTHORITY);
        }
        reqDto.setUserId(userID);
        mailTemplateService.importTemplate(reqDto);
        return ResponseResult.success();
    }

    @PostMapping("save")
    public Result saveTemplate(@RequestBody(required = false) MailTemplate reqDto) {
        String userID = Session.currentSession().getUserID();
        if (reqDto.getType().equals(MailTemplateTypeEnums.SYSTEM.getCode()) && !userService.isAdmin(userID)) {
            return ResponseResult.failure(NO_AUTHORITY);
        }
        reqDto.setUserID(userID);
        mailTemplateService.saveTemplate(reqDto);
        return ResponseResult.success();
    }

    @PostMapping("delete")
    public Result deleteTemplate(@RequestBody(required = false) UpdateMailTemplateReqDto reqDto) {
        mailTemplateService.deleteManyByIds(reqDto.getIds());
        return ResponseResult.success();
    }

    @PostMapping("setGroup")
    public Result setGroup(@RequestBody(required = false) UpdateMailTemplateGroupReqDto reqDto) {
        mailTemplateService.updateGroup(reqDto.getIds(), reqDto.getGroupId());
        return ResponseResult.success();
    }

    @PostMapping("updateStatus")
    public Result updateStatus(@RequestBody(required = false) UpdateMailTemplateReqDto reqDto) {
        mailTemplateService.updateStatus(reqDto.getIds(), reqDto.getStatus());
        return ResponseResult.success();
    }

    @PostMapping("export")
    public void export(String ids, Integer type, HttpServletRequest request, HttpServletResponse response) throws IOException {
        MailTemplateListDTO mailTemplateListDTO = new MailTemplateListDTO();
        mailTemplateListDTO.setFilters(new HashMap<>());
        if (StringUtils.isNotBlank(ids)) {
            List<String> _ids = Arrays.stream(ids.split(",")).toList();
            mailTemplateListDTO.getFilters().put("_id", Map.of("$in", _ids));
        }
//        if (StringUtils.isNotEmpty(groupID)) {
//            mailTemplateListDTO.getFilters().put("groupID", groupID);
//        }
        if (type != null) {
            mailTemplateListDTO.getFilters().put("type", type);
            if (type.equals(MailTemplateTypeEnums.USER.getCode())) {
                String userID = Session.currentSession().getUserID();
                mailTemplateListDTO.getFilters().put("userID", userID);
            }
        }
        List<MailTemplate> mailTemplates = mailTemplateService.find(mailTemplateListDTO);
        List<String> headers = new ArrayList<>();
        headers.add("模板名称");
        headers.add("标题");
        headers.add("内容");
        List<String> groupIds = mailTemplates.stream().map(MailTemplate::getGroupID).filter(StringUtils::isNotEmpty).toList();
        List<MailTemplateGroup> groups = mailTemplateGroupRepository.findByIdsIn(groupIds);
        Map<String, String> map = groups.stream().collect(Collectors.toMap(MailTemplateGroup::get_id, MailTemplateGroup::getGroupName));
        List<List<String>> data = mailTemplates.stream().map(e -> {
            List<String> row = new ArrayList<String>();
            row.add(e.getName());
//            row.add(map.get(e.getGroupID()));
//            row.add(String.valueOf(e.getUseCount()));
//            if (MailTemplateStatusEnums.fromCode(e.getStatus()) != null) {
//                row.add(MailTemplateStatusEnums.fromCode(e.getStatus()).getDescription());
//            } else {
//                row.add(String.valueOf(e.getStatus()));
//            }
            row.add(e.getTitle());
            row.add(e.getContent());
//            row.add(e.getCreateTime() != null ? DateUtil.formatByDate(e.getCreateTime(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM) : "");
//            row.add(e.getLastModifiedDate() != null ? DateUtil.formatByDate(e.getLastModifiedDate(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM) : "");
            return row;
        }).toList();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // 写数据
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i);
            List<String> rowData = data.get(i);
            for (int j = 0; j < rowData.size(); j++) {
                String text = rowData.get(j);
                if (text.length() > 32767) {
                    text = text.substring(0, 32767);
                }
                row.createCell(j).setCellValue(text);
            }
        }

        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        response.setHeader("Cache-Control", "public");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + UriEncoder.encode("模板导出.xlsx"));
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        // 输出文件
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            outputStream.flush();
        }
        workbook.close();
    }

    @PostMapping("/group/{pageSize}/{page}")
    public Result<PageResult<MailTemplateGroup>> listGroup(@PathVariable("pageSize") int pageSize,
                                                 @PathVariable("page") int page,
                                                           @RequestBody(required = false) MailTemplateListDTO mailTemplateListDTO) {
        String userID = Session.currentSession().getUserID();
        if (mailTemplateListDTO.getFilters().containsKey("type") && mailTemplateListDTO.getFilters().get("type").equals(
                MailTemplateTypeEnums.USER.getCode()
        )) {
            mailTemplateListDTO.getFilters().put("userID", userID);
        }
        PageResult<MailTemplateGroup> pageResult = mailTemplateGroupRepository.findByPagination(mailTemplateListDTO, pageSize, page);
        return ResponseResult.success(pageResult);
    }

    @PostMapping("/group/add")
    public Result addGroup(@RequestBody(required = false) MailTemplateGroup reqDto) {
        String userID = Session.currentSession().getUserID();
        if (reqDto.getType().equals(MailTemplateTypeEnums.SYSTEM.getCode()) && !userService.isAdmin(userID)) {
            return ResponseResult.failure(NO_AUTHORITY);
        }
        reqDto.setUserID(userID);
        mailTemplateGroupRepository.save(reqDto);
        return ResponseResult.success();
    }

    @PostMapping("/group/updateName")
    public Result updateGroupName(@RequestBody(required = false) MailTemplateGroup reqDto) {
        String userID = Session.currentSession().getUserID();
        if (reqDto.getType().equals(MailTemplateTypeEnums.SYSTEM.getCode()) && !userService.isAdmin(userID)) {
            return ResponseResult.failure(NO_AUTHORITY);
        }
        mailTemplateGroupRepository.updateGroupName(reqDto.get_id(), reqDto.getGroupName());
        return ResponseResult.success();
    }

    @PostMapping("/group/delete")
    public Result deleteGroup(@RequestBody() DeleteMailTemplateGroupReqDto reqDto) {
        String userID = Session.currentSession().getUserID();
        List<MailTemplateGroup> olds = mailTemplateGroupRepository.findByIdsIn(reqDto.getIds());
        if (!CollectionUtils.isEmpty(olds) && olds.stream().anyMatch(old ->
                old.getType().equals(MailTemplateTypeEnums.USER.getCode()) &&
                        !old.getUserID().equals(userID))) {
            return ResponseResult.failure(NO_AUTHORITY);
        }
        if (!CollectionUtils.isEmpty(olds) && olds.stream().anyMatch(old ->
                old.getType().equals(MailTemplateTypeEnums.SYSTEM.getCode()) &&
                        !userService.isAdmin(userID))) {
            return ResponseResult.failure(NO_AUTHORITY);
        }
        MailTemplateListDTO mailTemplateListDTO = new MailTemplateListDTO();
        mailTemplateListDTO.setFilters(new HashMap<>());
        mailTemplateListDTO.getFilters().put("groupID", Map.of("$in", reqDto.getIds()));
        List<MailTemplate> byPagination = mailTemplateService.find(mailTemplateListDTO);
        if (!byPagination.isEmpty()) {
            return ResponseResult.failure(MAIL_TEMPLATE_GROUP_IN_USE);
        }
        mailTemplateGroupRepository.deleteManyByIds(reqDto.getIds());
        return ResponseResult.success();
    }
}

