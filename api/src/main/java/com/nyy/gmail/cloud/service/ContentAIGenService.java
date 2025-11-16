package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.AccountTypeEnums;
import com.nyy.gmail.cloud.enums.GroupTaskStatusEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ContentAIGenService {

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountGroupRepository accountGroupRepository;

    @Autowired
    private ParamsService paramsService;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private MailTemplateRepository mailTemplateRepository;

    public void save(ContentAIGenReqDto reqDTO, String userID) {
        if (StringUtils.isEmpty(reqDTO.getDesc())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (StringUtils.isEmpty(reqDTO.getSubject())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (StringUtils.isEmpty(reqDTO.getContent())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (reqDTO.getSendNum() == null || reqDTO.getSendNum() <= 0) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (reqDTO.getGenNum() == null || reqDTO.getGenNum() <= 0) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (reqDTO.getGenNum() > 100) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "AI生成数量过多");
        }
        if (reqDTO.getSendNum() > 100) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "发送邮件数量过多");
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

        int i = reqDTO.getGenNum();

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

        taskUtil.createGroupTask(ids, TaskTypesEnums.EmailContentAiGen, new HashMap(Map.of(
                "accountUserID", accountUserID,
                "systemEmail", emails, // 种子邮箱
                "sendEmailIntervalInSecond", sendEmailIntervalInSecond,
                "taskDesc",reqDTO.getDesc() == null ? "" : reqDTO.getDesc(),
                "publishTotalCount", i * reqDTO.getSendNum(),
                "addDatas", "",
                "addMethod", "1",
                "filepath", "",
                "reqDto", reqDTO)), userID, "1", new Date());

        if (StringUtils.isNotBlank(reqDTO.getTemplateId())) {
            MailTemplate mailTemplate = mailTemplateRepository.findById(reqDTO.getTemplateId());
            if (mailTemplate != null) {
                mailTemplate.setUseCount(mailTemplate.getUseCount() + 1);
                mailTemplateRepository.update(mailTemplate);
            }
        }
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

    public PageResult<ContentAiGenDetail> detail(Map sorts, String groupTaskId, Integer pageSize, Integer pageNum) {
        PageResult<ContentAiGenDetail>  pageResult = new PageResult<>();
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
        Map<String, ContentAiGenDetail>  contentAiGenDetailHashMap = new HashMap<>();
        for (SubTask subTask : subTasks) {
            String content = subTask.getParams().getOrDefault("content", "").toString();
            String title = subTask.getParams().getOrDefault("title", "").toString();
            content = "<strong>subject:</strong>" + title + "<br>" + content;
            ContentAiGenDetail detail = null;
            if (contentAiGenDetailHashMap.containsKey(content)) {
                detail = contentAiGenDetailHashMap.get(content);
            } else {
                detail = new ContentAiGenDetail();
                detail.setContent(content);
                detail.setCreateTime(subTask.getCreateTime());
                detail.setFinishTime(subTask.getFinishTime());
                contentAiGenDetailHashMap.put(content, detail);
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

        Stream<ContentAiGenDetail> detailStream = contentAiGenDetailHashMap.values().stream();
        if (sorts != null) {
            if (sorts.containsKey("rate")) {
                if (sorts.get("rate").toString().equals("1")) {
                    detailStream = detailStream.sorted(Comparator.comparing(ContentAiGenDetail::getRate));
                } else if (sorts.get("rate").toString().equals("-1")) {
                    detailStream = detailStream.sorted((a,b) -> b.getRate().compareTo(a.getRate()));
                }
            }
        }
        AtomicInteger i =  new AtomicInteger(1);
        detailStream = detailStream.peek(detail -> detail.setNo(i.getAndIncrement()));
        detailStream = detailStream.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        pageResult.setData(detailStream.toList());
        pageResult.setPages((contentAiGenDetailHashMap.size() - 1) / pageSize + 1);
        pageResult.setTotal((long) contentAiGenDetailHashMap.size());

        return pageResult;
    }
}
