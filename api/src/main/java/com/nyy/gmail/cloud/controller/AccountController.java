package com.nyy.gmail.cloud.controller;


import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.pagination.JpaPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.AccountTypeEnums;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.repository.mongo.AccountGroupRepository;
import com.nyy.gmail.cloud.service.AccountService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
//@RequiredPermission(MenuType.account)
@RequestMapping({"/api/account", "/api/consumer/account"})
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Resource
    private JpaPaginationHelper jpaPaginationHelper;

    @Autowired
    private AccountGroupRepository accountGroupRepository;

    @PostMapping("{pageSize}/{page}")
    public Result<PageResult<Account>> list(@PathVariable("pageSize") int pageSize,
                                            @PathVariable("page") int page,
                                            @RequestBody(required = false) AccountListDTO accountListDTO) {
        String userID = Session.currentSession().getUserID();
        if(accountListDTO.getFilters() == null) {
            accountListDTO.setFilters(new HashMap<>());
        }
        accountListDTO.getFilters().put("userID", userID);
        PageResult<Account> pageResult = accountService.findAccountByPagination(accountListDTO, pageSize, page);//按id倒排序，未做到populate
        Result<PageResult<Account>> result = ResponseResult.success(pageResult);
        List<String> ids = result.getData().getData().stream().map(Account::getGroupID).filter(StringUtils::isNotEmpty).toList();

        List<AccountGroup> groups = accountGroupRepository.findByIdListAndUserID(ids, userID);
        Map<String, String> map = groups.stream().collect(Collectors.toMap(AccountGroup::get_id, AccountGroup::getGroupName));

        result.getData().getData().forEach(e -> {
            if (StringUtils.isNotEmpty(e.getGroupID()) && map.containsKey(e.getGroupID())) {
                e.setGroupName(map.get(e.getGroupID()));
            }
            e.setSession("");
            e.setLoginSession("");
            e.setLstBindingKeyAlias("");
            e.setGoogleAccountDataStore("");
            e.setToken("");
            e.setProxyIp("");
            e.setProxyPort("");
            e.setProxyPassword("");
            e.setProxyUsername("");
            e.setDeviceinfo("");
            e.setDevice("");
        });
        return result;
    }

    @PostMapping("delete")
    public Result delete(@RequestBody(required = false) AccountListDTO accountListDTO) {
        accountService.delete(accountListDTO.getIds(), Session.currentSession().userID, false);
        return ResponseResult.success();
    }

    // 1. 导入
    @PostMapping("importAccount")
    public Result importAccount(@RequestBody(required = false) ImportAccountReqDto reqDto) {
        accountService.importAccount(reqDto, Session.currentSession().userID);
        return ResponseResult.success();
    }

    @PostMapping("export")
    public void export(String phone, String onlineStatus, String email, String platform, String platform2, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userID = Session.currentSession().getUserID();
        AccountListDTO accountListDTO = new AccountListDTO();
        accountListDTO.setFilters(new HashMap<>());
        if (StringUtils.isNotEmpty(phone)) {
            accountListDTO.getFilters().put("phone", phone);
        }
        if (StringUtils.isNotEmpty(onlineStatus)) {
            accountListDTO.getFilters().put("onlineStatus", onlineStatus);
        }
        if (StringUtils.isNotEmpty(email)) {
            accountListDTO.getFilters().put("email", email);
        }
        if (StringUtils.isNotEmpty(platform)) {
            accountListDTO.getFilters().put("platform", platform);
        }
        if (StringUtils.isNotEmpty(platform2)) {
            accountListDTO.getFilters().put("platform2", platform2);
        }
        String createTimeRange1 = request.getParameter("createTimeRange.0");
        String createTimeRange2 = request.getParameter("createTimeRange.1");
        if (StringUtils.isNotEmpty(createTimeRange1) &&  StringUtils.isNotEmpty(createTimeRange2)) {
            accountListDTO.getFilters().put("createTimeRange", List.of(createTimeRange1, createTimeRange2));
        }
        String limitSendEmail = request.getParameter("limitSendEmail.$in.1");
        if (StringUtils.isEmpty(limitSendEmail)) {
            limitSendEmail = request.getParameter("limitSendEmail");
        }
        if (StringUtils.isNotEmpty(limitSendEmail)) {
            if (limitSendEmail.equals("false")) {
                accountListDTO.getFilters().put("limitSendEmail", "0");
            } else {
                accountListDTO.getFilters().put("limitSendEmail", "1");
            }
        }
        accountListDTO.getFilters().put("type", Map.of("$ne", AccountTypeEnums.sendgrid.getCode()));
        List<Account> accountList = accountService.findAllIncludePart(userID, accountListDTO);

        String [] headers = {"邮箱", "密码", "手机号", "已接码数", "已导出数", "状态", "是否已检测", "每天已发邮件数", "发邮件是否限制", "累计发件数", "登录错误信息", "被封时间", "创建时间"};
        List<String []> data = accountList.stream().map(e -> new String[]{
                e.getEmail(),
                e.getPassword(),
                e.getPhone() == null ? "" : e.getPhone(),
                e.getRealUsedPlatformIds() == null ? "0" : e.getRealUsedPlatformIds().size() + "",
                e.getUsed() == null ? "0" : e.getUsed() + "",
                e.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode()) ? "可用":"不可用",
                e.getIsCheck() != null && e.getIsCheck() ? "已检测" : "未检测",
                e.getSendEmailNumByDayDisplay() == null ? "0" : e.getSendEmailNumByDayDisplay() + "",
                e.getLimitSendEmail() != null && e.getLimitSendEmail() ? "限制" : "",
                e.getSendEmailTotal() == null ? "0" : e.getSendEmailTotal() + "",
                e.getLoginError(),
                e.getOnlineStatus().equals(AccountOnlineStatus.OFFLINE.getCode()) ? DateUtil.formatByDate(e.getChangeOnlineStatusTime(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM) : "",
                e.getCreateTime() != null ? DateUtil.formatByDate(e.getCreateTime(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM) : "",
        }).toList();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // 写表头
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // 写数据
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            String[] rowData = data.get(i);
            for (int j = 0; j < rowData.length; j++) {
                row.createCell(j).setCellValue(rowData[j]);
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setHeader("Cache-Control", "public");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + UriEncoder.encode("account_"+ data.size() +".xlsx"));
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        // 输出文件
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            outputStream.flush();
        }
        workbook.close();
    }

    @PostMapping("retryCheck")
    public Result retryCheck(@RequestBody(required = false) IdsListDTO reqDto) {
        accountService.retryCheck(reqDto, Session.currentSession().userID);
        return ResponseResult.success();
    }

    @PostMapping("setGroup")
    public Result setGroup(@RequestBody(required = false) Map reqDto) {
        accountService.setGroup(reqDto, Session.currentSession().userID);
        return ResponseResult.success();
    }


    @PostMapping("/saveSendGrid")
    public Result saveSendGrid(@RequestBody SendGridAccountDto sendGridAccountDto) {
        String userID = Session.currentSession().getUserID();
        sendGridAccountDto.setUserID(userID);
        accountService.saveSendGrid(sendGridAccountDto);
        return ResponseResult.success();
    }

    @PostMapping("importSendGrid")
    public Result importPlatform(@RequestParam("file") MultipartFile file) throws IOException {
        String data = new String(file.getBytes(), StandardCharsets.UTF_8);
        data = data.replace("\r", "");
        int batchSize = 500;
        List<Account> entities = new ArrayList<>(batchSize);
        int repeat = 0;
        String[] lines = data.split("\n");
        int total = lines.length;
        for (String line : lines) {
            String[] sv = line.split("----", -1);
            Map<String, String> map = new HashMap<>();
            String email =  sv[0].trim();
            String apiKey =  sv[1].trim();
            String userID = Session.currentSession().getUserID();

            Account account = new Account();
            account.setType(AccountTypeEnums.sendgrid.getCode());
            account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
            account.setChangeOnlineStatusTime(new Date());
            account.setAccID(email);
            account.setEmail(email);
            account.setSendGridApiKey(apiKey);
            account.setUserID(userID);
            account.setIsCheck(true);
            account.setCreateTime(new Date());

            if (entities.stream().noneMatch(e -> e.getSendGridApiKey().equals(account.getSendGridApiKey()))) {
                entities.add(account);
            }
            if (entities.size() >= batchSize) {
                int success = accountService.saveBatch(entities);
                repeat += entities.size() - success;
                entities = new ArrayList<>();
            }
        }
        if (!entities.isEmpty()) {
            int success = accountService.saveBatch(entities);
            repeat += entities.size() - success;
        }

        return ResponseResult.success();
    }

    @PostMapping("/saveWorkspaceAccount")
    public Result saveWorkspaceAccount(@RequestBody WorkspaceAccountDto workspaceAccountDto) {
        String userID = Session.currentSession().getUserID();
        workspaceAccountDto.setUserID(userID);
        accountService.saveWorkspace(workspaceAccountDto);
        return ResponseResult.success();
    }

    @PostMapping("/saveYahooAccount")
    public Result saveYahooAccount(@RequestBody ImportAccountReqDto reqDto) {
        accountService.saveYahoo(reqDto);
        return ResponseResult.success();
    }
}

