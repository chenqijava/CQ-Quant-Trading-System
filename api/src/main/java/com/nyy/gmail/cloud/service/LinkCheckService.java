package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.configuration.PathConfig;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.repository.mongo.AccountGroupRepository;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class LinkCheckService {

    @Autowired
    private ParamsService paramsService;

    @Autowired
    private PathConfig pathConfig;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountGroupRepository accountGroupRepository;

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    public GroupTask save(LinkCheckReqDto reqDTO, String userID) {
        if (StringUtils.isEmpty(reqDTO.getDesc())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (StringUtils.isEmpty(reqDTO.getAddMethod())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (reqDTO.getAddMethod().equals("1") && StringUtils.isEmpty(reqDTO.getAddData())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (reqDTO.getAddMethod().equals("2") &&  StringUtils.isEmpty(reqDTO.getFilepath())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (reqDTO.getSendNum() == null || reqDTO.getSendNum() <= 0) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (reqDTO.getSendNum() > 100) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "发送邮件数量过多");
        }
        // 判断是否
        String linkCheckTemplate = paramsService.getParams("account.linkCheckTemplate", null, null).toString();
        String linkCheckSubjectTemplate = paramsService.getParams("account.linkCheckSubjectTemplate", null, null).toString();

        if (StringUtils.isEmpty(linkCheckTemplate)) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "请配置\"链接检测配置：检测邮件内容模版\"");
        }
        if (StringUtils.isEmpty(linkCheckSubjectTemplate)) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "请配置\"链接检测配置：检测邮件主题模版\"");
        }

        if (!FileUtils.exists(FileUtils.resPath.resolve(linkCheckTemplate).toString())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "文件不存在，请重新配置\"链接检测配置：检测邮件内容模版\"");
        }
        if (!FileUtils.exists(FileUtils.resPath.resolve(linkCheckSubjectTemplate).toString())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "文件不存在，请重新配置\"链接检测配置：检测邮件主题模版\"");
        }

        // 判断账号
        String accountUserID = userID;
        Set<MenuType> permissionSet = userService.getPermissionSet(userID);
        if (!permissionSet.contains(MenuType.accountGroup)) {
            accountUserID = Constants.ADMIN_USER_ID;
        }

        // 查询种子邮箱
        List<String> emails = new ArrayList<>();
        AccountListDTO accountListDTO = new AccountListDTO();
        accountListDTO.setFilters(new HashMap<>(Map.of("userID", accountUserID, "onlineStatus", AccountOnlineStatus.ONLINE.getCode(), "isCheck", true)));

        AccountGroup group = accountGroupRepository.findByGroupNameAndUserID("种子邮箱分组", accountUserID);
        if (group != null) {
            accountListDTO.getFilters().put("groupID", group.get_id());
        }
        accountListDTO.getFilters().put("type", Map.of("$ne", AccountTypeEnums.sendgrid.getCode()));
        accountListDTO.setSorter(new HashMap<>(Map.of("lastPullMessageTime", -1)));
        PageResult<Account> accountPageResult = accountRepository.findByPagination(accountListDTO, reqDTO.getSendNum(), 1);
        emails = accountPageResult.getData().stream().map(Account::getEmail).collect(Collectors.toList());
        if (emails.size() < reqDTO.getSendNum()) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "种子邮箱分组数量不足" + reqDTO.getSendNum());
        }

        int i = 0;
        if (reqDTO.getAddMethod().equals("1")) {
            i = reqDTO.getAddData().split("\n").length;
        } else if (reqDTO.getAddMethod().equals("2")) {
            Path resPath = FileUtils.resPath;
            Path path = resPath.resolve(reqDTO.getFilepath()).toAbsolutePath().normalize();
            i = FileUtils.readCsvFileLineCount(path.toString());
        }

        String sendEmailMaxNumByDay = paramsService.getParams("account.sendEmailMaxNumByDay", null, null).toString();
        // 所有邮箱
        accountListDTO = new AccountListDTO();
        accountListDTO.setFilters(new HashMap<>(Map.of("limitSendEmail", Map.of("$ne", true), "userID", accountUserID, "onlineStatus", AccountOnlineStatus.ONLINE.getCode(), "isCheck", true, "sendEmailNumByDay", Map.of("$lt", Integer.parseInt(sendEmailMaxNumByDay)))));
        group = accountGroupRepository.findByGroupNameAndUserID("群发邮件分组", accountUserID);
        if (group != null) {
            accountListDTO.getFilters().put("groupID", group.get_id());
        }
        accountListDTO.getFilters().put("type", Map.of("$ne",AccountTypeEnums.sendgrid.getCode()));
        accountListDTO.setSorter(new HashMap<>(Map.of("lastPullMessageTime", -1)));
        accountPageResult = accountRepository.findByPagination(accountListDTO, 2000, 1);
        if (accountPageResult.getData().isEmpty()) {
            throw new CommonException(ResultCode.NO_ONLINE_ACCOUNT);
        }

        List<String> ids = null;
        if (accountPageResult.getData().size() < reqDTO.getSendNum()) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "群发邮件分组数量不足" + reqDTO.getSendNum());
        } else {
            Collections.shuffle(accountPageResult.getData());
            ids = accountPageResult.getData().subList(0, Math.min(reqDTO.getSendNum(), 2000)).stream().map(Account::get_id).collect(Collectors.toList());
        }

        String sendEmailIntervalInSecond = paramsService.getParams("account.sendEmailIntervalInSecond", null, null).toString();

        return taskUtil.createGroupTask(ids, TaskTypesEnums.EmailLinkCheck, new HashMap(Map.of(
                "accountUserID", accountUserID,
                "systemEmail", emails, // 种子邮箱
                "sendEmailIntervalInSecond", sendEmailIntervalInSecond,
                "taskDesc",reqDTO.getDesc() == null ? "" : reqDTO.getDesc(),
                "publishTotalCount", i * reqDTO.getSendNum(),
                "addDatas", reqDTO.getAddData() == null ? "" : Arrays.stream(reqDTO.getAddData().split("\n")).toList(),
                "addMethod", reqDTO.getAddMethod(),
                "filepath", reqDTO.getFilepath() == null ? "" : reqDTO.getFilepath(),
                "reqDto", reqDTO)), userID, "1", new Date());

    }

    public PageResult<GroupTask> findByPagination(Params params, Integer pageSize, Integer pageNum) {
        return groupTaskRepository.findByPagination(pageSize, pageNum, params.getFilters());
    }

    public void delete(IdsListDTO data) {
        for (String id : data.getIds()) {
            GroupTask groupTask = groupTaskRepository.findById(id).orElse(null);
            if (groupTask != null && groupTask.getUserID().equals(Session.currentSession().getUserID()) && (groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode()) || groupTask.getStatus().equals(GroupTaskStatusEnums.failed.getCode()))) {
                groupTaskRepository.delete(groupTask);
            }
        }
    }

    public void stop(IdsListDTO data) {
        for (String id : data.getIds()) {
            GroupTask groupTask = groupTaskRepository.findById(id).orElse(null);
            if (groupTask != null && groupTask.getUserID().equals(Session.currentSession().getUserID()) && (!groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode()) && !groupTask.getStatus().equals(GroupTaskStatusEnums.failed.getCode()))) {
                groupTask.setStatus(GroupTaskStatusEnums.init.getCode());
                groupTaskRepository.save(groupTask);
                taskUtil.finishForceTaskByPublish(groupTask);
            }
        }
    }

    public PageResult<LinkCheckDetail> detail(Map sorts, String groupTaskId, Integer pageSize, Integer pageNum) {
        PageResult<LinkCheckDetail>  pageResult = new PageResult<>();
        GroupTask groupTask = groupTaskRepository.findById(groupTaskId).orElse(null);
        if (groupTask == null) {
            pageResult.setPageNum(pageNum);
            pageResult.setPageSize(pageSize);
            pageResult.setData(new ArrayList<>());
            pageResult.setPages(0);
            pageResult.setTotal(0L);
            return pageResult;
        }

        List<SubTask> subTasks = subTaskRepository.findByGroupTaskId(groupTaskId);
        Map<String, LinkCheckDetail>  linkCheckDetailMap = new HashMap<>();
        for (SubTask subTask : subTasks) {
            String link = subTask.getParams().getOrDefault("addData", "").toString();
            LinkCheckDetail detail = null;
            if (linkCheckDetailMap.containsKey(link)) {
                detail = linkCheckDetailMap.get(link);
            } else {
                detail = new LinkCheckDetail();
                detail.setLink(link);
                detail.setCreateTime(subTask.getCreateTime());
                detail.setFinishTime(subTask.getFinishTime());
                linkCheckDetailMap.put(link, detail);
            }
            if (subTask.getCreateTime() != null) {
                if (detail.getCreateTime() == null || detail.getCreateTime().after(subTask.getCreateTime())) {
                    detail.setCreateTime(subTask.getCreateTime());
                }
            }
            if (subTask.getFinishTime() != null) {
                if (detail.getFinishTime() == null || detail.getFinishTime().before(subTask.getFinishTime())) {
                    detail.setFinishTime(subTask.getFinishTime());
                }
            }
            if (subTask.getStatus().equals(GroupTaskStatusEnums.success.getCode())) {
                detail.setSuccess(detail.getSuccess() + 1);
                if (subTask.getResult() != null && subTask.getResult().containsKey("labels")) {
                    List<String> labels = Arrays.stream(subTask.getResult().getOrDefault("labels", "").toString().split(",")).toList();
                    if (labels.contains("^s")) {
                        detail.setJunkNum(detail.getJunkNum() + 1);
                    } else {
                        detail.setNormalNum(detail.getNormalNum() + 1);
                    }
                } else {
                    detail.setUnknownNum(detail.getUnknownNum() + 1);
                }
            } else if (subTask.getStatus().equals(GroupTaskStatusEnums.failed.getCode())) {
                detail.setFailed(detail.getSuccess() + 1);
            } else {
                detail.setOther(detail.getOther() + 1);
                detail.setStatus("doing");
            }
        }


        Stream<LinkCheckDetail> detailStream = linkCheckDetailMap.values().stream().skip((long) (pageNum - 1) * pageSize).limit(pageSize);
        if (sorts != null) {
            if (sorts.containsKey("rate")) {
                if (sorts.get("rate").toString().equals("1")) {
                    detailStream = detailStream.sorted(Comparator.comparing(LinkCheckDetail::getRate));
                } else if (sorts.get("rate").toString().equals("-1")) {
                    detailStream = detailStream.sorted((a,b) -> b.getRate().compareTo(a.getRate()));
                }
            }
        }
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        pageResult.setData(detailStream.toList());
        pageResult.setPages((linkCheckDetailMap.size() - 1) / pageSize + 1);
        pageResult.setTotal((long) linkCheckDetailMap.size());

        return pageResult;
    }
}
