package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BatchSendEmailService {

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BalanceDetailRepository balanceDetailRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ParamsService paramsService;

    @Autowired
    private SendEmailEventMonitorRepository sendEmailEventMonitorRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private MessageRepository  messageRepository;

    @Autowired
    private SendEmailEventTrackingRepository sendEmailEventTrackingRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountGroupRepository accountGroupRepository;

    public void save(BatchSendEmailReqDto reqDTO, String userID) {
        // 判断余额
        if (StringUtils.isEmpty(reqDTO.getTaskName())) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }
        if (StringUtils.isEmpty(reqDTO.getTitle())) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }
        if (StringUtils.isEmpty(reqDTO.getContent())) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }
        if (reqDTO.getAddMethod().equals("1") && StringUtils.isEmpty(reqDTO.getAddData())) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }
        if (reqDTO.getAddMethod().equals("2") && StringUtils.isEmpty(reqDTO.getFilepath())) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }
        if (reqDTO.getAddMethod().equals("3") && (StringUtils.isEmpty(reqDTO.getTaskId()) || StringUtils.isEmpty(reqDTO.getAction()))) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }
        if (reqDTO.getTestAB().equals("yes")) {
            if (StringUtils.isEmpty(reqDTO.getTitleB()) ||  StringUtils.isEmpty(reqDTO.getContentB()) || reqDTO.getPercent() == null || reqDTO.getPercent() < 10 || reqDTO.getPercent() > 90 || reqDTO.getTestTimeLengthHour() == null || StringUtils.isEmpty(reqDTO.getFactor())) {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID);
            }
            try {
                double v = Double.parseDouble(reqDTO.getTestTimeLengthHour());
            } catch (NumberFormatException e) {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID);
            }
        }

        String accountUserID = userID;
        Set<MenuType> permissionSet = userService.getPermissionSet(userID);
        if (!permissionSet.contains(MenuType.accountGroup)) {
            accountUserID = Constants.ADMIN_USER_ID;
        }

        // 查询种子邮箱
        List<String> emails = new ArrayList<>();
        if (reqDTO.getSystemEmailCount() != null &&  reqDTO.getSystemEmailCount() > 0) {
            AccountListDTO accountListDTO = new AccountListDTO();
            accountListDTO.setFilters(new HashMap<>(Map.of("userID", accountUserID, "onlineStatus", AccountOnlineStatus.ONLINE.getCode(), "isCheck", true)));

            AccountGroup group = accountGroupRepository.findByGroupNameAndUserID("种子邮箱分组", accountUserID);
            if (group != null) {
                accountListDTO.getFilters().put("groupID", group.get_id());
            }
            accountListDTO.getFilters().put("type", Map.of("$ne",AccountTypeEnums.sendgrid.getCode()));
            accountListDTO.setSorter(new HashMap<>(Map.of("lastPullMessageTime", -1)));
            PageResult<Account> accountPageResult = accountRepository.findByPagination(accountListDTO, reqDTO.getSystemEmailCount(), 1);
            emails = accountPageResult.getData().stream().map(Account::getEmail).collect(Collectors.toList());
        }

        // 外部种子邮箱
        if (reqDTO.getOtherEmails() != null) {
            emails.addAll(Arrays.stream(reqDTO.getOtherEmails().split("\n")).filter(StringUtils::isNotEmpty).toList());
        }

        int i = 0;
        if (reqDTO.getAddMethod().equals("1")) {
            i = reqDTO.getAddData().split("\n").length;
        } else if (reqDTO.getAddMethod().equals("2")) {
            Path resPath = FileUtils.resPath;
            Path path = resPath.resolve(reqDTO.getFilepath()).toAbsolutePath().normalize();
            i = FileUtils.readCsvFileLineCount(path.toString());
        } else if (reqDTO.getAddMethod().equals("3")) {
            List<SendEmailEventMonitor> sendEmailEventMonitors = sendEmailEventMonitorRepository.findByGroupTaskIdAndAction(reqDTO.getTaskId(), reqDTO.getAction());
            List<String> subTaskIds = sendEmailEventMonitors.stream().map(SendEmailEventMonitor::getSubTaskId).toList();

            List<SubTask> subTaskList = subTaskRepository.findByIdIn(subTaskIds);
            reqDTO.setAddData(String.join("\n", subTaskList.stream().filter(e -> e.getStatus().equals(SubTaskStatusEnums.success.getCode())).map(e -> e.getParams().get("addData").toString()).toList()));
            i = subTaskList.size();
        }

        if (i < 50 && reqDTO.getTestAB().equals("yes")) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID, "发送邮件数量小于50，不支持A/B测");
        }
        // 扣费加上种子邮箱
        i = i + emails.size();

        String sendEmailMaxNumByDay = paramsService.getParams("account.sendEmailMaxNumByDay", null, null).toString();
        // 所有邮箱
        AccountListDTO accountListDTO = new AccountListDTO();
        accountListDTO.setFilters(new HashMap<>(Map.of("limitSendEmail", Map.of("$ne", true), "userID", accountUserID, "onlineStatus", AccountOnlineStatus.ONLINE.getCode(), "isCheck", true, "sendEmailNumByDay", Map.of("$lt", Integer.parseInt(sendEmailMaxNumByDay)))));
        AccountGroup group = accountGroupRepository.findByGroupNameAndUserID("群发邮件分组", accountUserID);
        if (group != null) {
            accountListDTO.getFilters().put("groupID", group.get_id());
        }
        if (reqDTO.getType() != null && reqDTO.getType().equals("api")) {
            accountListDTO.getFilters().put("type", AccountTypeEnums.sendgrid.getCode());
        } else {
            accountListDTO.getFilters().put("type", Map.of("$ne",AccountTypeEnums.sendgrid.getCode()));
        }
        accountListDTO.setSorter(new HashMap<>(Map.of("lastPullMessageTime", -1)));
        PageResult<Account> accountPageResult = accountRepository.findByPagination(accountListDTO, 2000, 1);
        if (accountPageResult.getData().isEmpty()) {
            throw new CommonException(ResultCode.NO_ONLINE_ACCOUNT);
        }

        User one = userRepository.findOneByUserID(userID);
        if (one == null) {
            throw new CommonException(ResultCode.DATA_NOT_EXISTED);
        }
        if (one.getRestSendEmailCount().compareTo(BigDecimal.valueOf(i)) < 0) {
            throw new CommonException(ResultCode.USER_BALANCE_INSUFFICIENT);
        }
        RLock lock = redissonClient.getLock(userID + "-charge");
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    User user = userRepository.findOneByUserID(userID);
                    if (user.getRestSendEmailCount().compareTo(BigDecimal.valueOf(i)) < 0) {
                        throw new CommonException(ResultCode.USER_BALANCE_INSUFFICIENT);
                    }
                    user.setRestSendEmailCount(user.getRestSendEmailCount().subtract(BigDecimal.valueOf(i)));
                    userRepository.updateUser(user);
                    balanceDetailRepository.addUserBill("任务扣款",
                            user.getUserID(), BigDecimal.valueOf(i), user.getRestSendEmailCount(), user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.DEDUCT_SEND_EMAIL_COUNT);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
        }

        List<String> ids = null;
        if (accountPageResult.getData().size() <= i) {
            ids = accountPageResult.getData().stream().map(Account::get_id).collect(Collectors.toList());
        } else {
            Collections.shuffle(accountPageResult.getData());
            ids = accountPageResult.getData().subList(0, Math.min(i, 2000)).stream().map(Account::get_id).collect(Collectors.toList());
        }

        String sendEmailIntervalInSecond = paramsService.getParams("account.sendEmailIntervalInSecond", null, null).toString();

        taskUtil.createGroupTask(ids, TaskTypesEnums.BatchSendEmail, new HashMap(Map.of(
                "accountUserID", accountUserID,
                "systemEmail", emails, // 种子邮箱
                "sendEmailIntervalInSecond", sendEmailIntervalInSecond,
                "taskDesc",reqDTO.getTaskName() == null ? "" : reqDTO.getTaskName(),
                "publishTotalCount", i,
                "addDatas", reqDTO.getAddData() == null ? "" : Arrays.stream(reqDTO.getAddData().split("\n")).toList(),
                "addMethod", reqDTO.getAddMethod(),
                "filepath", reqDTO.getFilepath() == null ? "" : reqDTO.getFilepath(),
                "reqDto", reqDTO)), userID, reqDTO.getSendMethod(), reqDTO.getSendTime());
    }

    public GroupTask test(BatchSendEmailTestReqDto reqDTO, String userID) {
        // 判断余额
        if (StringUtils.isEmpty(reqDTO.getTaskName())) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }
        if (StringUtils.isEmpty(reqDTO.getTitle())) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }
        if (StringUtils.isEmpty(reqDTO.getContent())) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }
        if (reqDTO.getCount() == null || reqDTO.getCount() < 1) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }
        if (reqDTO.getTestAB().equals("yes")) {
            if (StringUtils.isEmpty(reqDTO.getTitleB()) ||  StringUtils.isEmpty(reqDTO.getContentB())) {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID);
            }
        }

        int i = reqDTO.getCount();

        boolean isABTest = reqDTO.getTestAB().equals("yes");

        String accountUserID = userID;
        Set<MenuType> permissionSet = userService.getPermissionSet(userID);
        if (!permissionSet.contains(MenuType.accountGroup)) {
            accountUserID = Constants.ADMIN_USER_ID;
        }

        String sendEmailMaxNumByDay = paramsService.getParams("account.sendEmailMaxNumByDay", null, null).toString();
        // 所有邮箱
        AccountListDTO accountListDTO = new AccountListDTO();
        accountListDTO.setFilters(new HashMap<>(Map.of("limitSendEmail", Map.of("$ne", true), "userID", accountUserID, "onlineStatus", AccountOnlineStatus.ONLINE.getCode(), "isCheck", true, "sendEmailNumByDay", Map.of("$lt", Integer.parseInt(sendEmailMaxNumByDay)))));
        AccountGroup group = accountGroupRepository.findByGroupNameAndUserID("群发邮件分组", accountUserID);
        if (group != null) {
            accountListDTO.getFilters().put("groupID", group.get_id());
        }
        accountListDTO.getFilters().put("type", Map.of("$ne",AccountTypeEnums.sendgrid.getCode()));
        accountListDTO.setSorter(new HashMap<>(Map.of("lastPullMessageTime", -1)));
        PageResult<Account> accountPageResult = accountRepository.findByPagination(accountListDTO, isABTest ? i * 4 : i * 2, 1);
        if (accountPageResult.getData().size() != (isABTest ? i * 4 : i * 2)) {
            throw new CommonException(ResultCode.NO_ENOUGH_ACCOUNT);
        }

        User one = userRepository.findOneByUserID(userID);
        if (one == null) {
            throw new CommonException(ResultCode.DATA_NOT_EXISTED);
        }
        if (one.getRestSendEmailCount().compareTo(BigDecimal.valueOf(isABTest ? i * 2L : i)) < 0) {
            throw new CommonException(ResultCode.USER_BALANCE_INSUFFICIENT);
        }
        RLock lock = redissonClient.getLock(userID + "-charge");
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    User user = userRepository.findOneByUserID(userID);
                    if (user.getRestSendEmailCount().compareTo(BigDecimal.valueOf(isABTest ? i * 2L : i)) < 0) {
                        throw new CommonException(ResultCode.USER_BALANCE_INSUFFICIENT);
                    }
                    user.setRestSendEmailCount(user.getRestSendEmailCount().subtract(BigDecimal.valueOf(isABTest ? i * 2L : i)));
                    userRepository.updateUser(user);
                    balanceDetailRepository.addUserBill("任务扣款",
                            user.getUserID(), BigDecimal.valueOf(isABTest ? i * 2L : i), user.getRestSendEmailCount(), user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.DEDUCT_SEND_EMAIL_COUNT);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
        }

        List<String> ids = accountPageResult.getData().subList(0, accountPageResult.getData().size() / 2).stream().map(Account::get_id).collect(Collectors.toList());
        reqDTO.setAddData(String.join("\n", accountPageResult.getData().subList(accountPageResult.getData().size() / 2, accountPageResult.getData().size()).stream().map(Account::getEmail).toList()));
        String sendEmailIntervalInSecond = paramsService.getParams("account.sendEmailIntervalInSecond", null, null).toString();

        GroupTask groupTask = taskUtil.createGroupTask(ids, TaskTypesEnums.BatchSendEmailTest, new HashMap(Map.of(
                "accountUserID", accountUserID,
                "sendEmailIntervalInSecond", sendEmailIntervalInSecond,
                "taskDesc", "[测试]" + (reqDTO.getTaskName() == null ? "" : reqDTO.getTaskName()),
                "publishTotalCount", isABTest ? i * 2L : i,
                "addDatas", reqDTO.getAddData() == null ? "" : Arrays.stream(reqDTO.getAddData().split("\n")).toList(),
                "addMethod", "1",
                "filepath", "",
                "reqDto", reqDTO)), userID, "1", null);

        if (StringUtils.isNotEmpty(reqDTO.getGroupTaskId())) {
            GroupTask task = groupTaskRepository.findById(reqDTO.getGroupTaskId()).orElse(null);
            if (task != null) {
                String testIds = task.getParams().getOrDefault("testIds", "").toString();
                if (StringUtils.isEmpty(testIds)) {
                    testIds = groupTask.get_id();
                } else {
                    testIds = testIds + "," + groupTask.get_id();
                }
                task.getParams().put("testIds", testIds);
                groupTaskRepository.save(task);
            }
        }

        return groupTask;
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

    public void pause(IdsListDTO data) {
        for (String id : data.getIds()) {
            GroupTask groupTask = groupTaskRepository.findById(id).orElse(null);
            if (groupTask != null && groupTask.getUserID().equals(Session.currentSession().getUserID()) && (!groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode()) && !groupTask.getStatus().equals(GroupTaskStatusEnums.failed.getCode()))) {
                groupTask.setStatus(GroupTaskStatusEnums.pause.getCode());
                groupTaskRepository.save(groupTask);
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

    public void start(IdsListDTO data) {
        for (String id : data.getIds()) {
            GroupTask groupTask = groupTaskRepository.findById(id).orElse(null);
            if (groupTask != null && groupTask.getUserID().equals(Session.currentSession().getUserID()) && (groupTask.getStatus().equals(GroupTaskStatusEnums.pause.getCode()))) {
                groupTask.setStatus(GroupTaskStatusEnums.init.getCode());
                if (groupTask.getExecuteTime() != null && groupTask.getExecuteTime().after(new Date())) {
                    groupTask.setStatus(GroupTaskStatusEnums.waitPublish.getCode());
                }
                groupTaskRepository.save(groupTask);
            }
        }
    }

    public List<SecondSendRespDto> secondSend() {
        PageResult<GroupTask> groupTaskPageResult = groupTaskRepository.findByPagination(1000, 1, new HashMap<>(Map.of("userID", Session.currentSession().userID, "type", TaskTypesEnums.BatchSendEmail.getCode())));
        List<String> ids = groupTaskPageResult.getData().stream().map(GroupTask::get_id).toList();
        List<SendEmailEventMonitor> sendEmailEventMonitorList = sendEmailEventMonitorRepository.findByGroupTaskIdIn(ids);

        return groupTaskPageResult.getData().stream().map(groupTask -> {
            SecondSendRespDto secondSendRespDto = new SecondSendRespDto();
            secondSendRespDto.setId(groupTask.get_id());
            secondSendRespDto.setName(groupTask.getDesc());
            secondSendRespDto.setOpen((int) sendEmailEventMonitorList.stream().filter(e->e.getEvent().equals("open") && e.getCount() > 0 && e.getGroupTaskId().equals(groupTask.get_id())).count());
            secondSendRespDto.setNoOpen((int) sendEmailEventMonitorList.stream().filter(e->e.getEvent().equals("open") && e.getCount() <= 0 && e.getGroupTaskId().equals(groupTask.get_id())).count());
            secondSendRespDto.setClick((int) sendEmailEventMonitorList.stream().filter(e->e.getEvent().equals("click") && e.getCount() > 0 && e.getGroupTaskId().equals(groupTask.get_id())).count());
            secondSendRespDto.setNoClick((int) sendEmailEventMonitorList.stream().filter(e->e.getEvent().equals("click") && e.getCount() <= 0 && e.getGroupTaskId().equals(groupTask.get_id())).count());
            secondSendRespDto.setReply((int) sendEmailEventMonitorList.stream().filter(e->e.getEvent().equals("reply") && e.getCount() > 0 && e.getGroupTaskId().equals(groupTask.get_id())).count());
            secondSendRespDto.setNoReply((int) sendEmailEventMonitorList.stream().filter(e->e.getEvent().equals("reply") && e.getCount() <= 0 && e.getGroupTaskId().equals(groupTask.get_id())).count());

            return secondSendRespDto;
        }).toList();
    }

    public GroupTask getDetailTest(String id) {
        return groupTaskRepository.findById(id).orElse(null);
    }

    public PageResult<SubTask> listTest(String id, Integer pageSize, Integer pageNum, Params params) {
        GroupTask groupTask = groupTaskRepository.findById(id).orElse(null);
        if (groupTask == null ||  !groupTask.getUserID().equals(Session.currentSession().getUserID())) {
            return new PageResult<>();
        }
        String[] split = groupTask.getParams().getOrDefault("testIds", "").toString().split(",");
        params.getFilters().put("groupTaskId", Map.of("$in", List.of(split)));
        return subTaskRepository.findByPagination(pageSize, pageNum, params.getFilters(), params.getSorter());
    }

    public BatchSendEmailDetailRespDto detail(String id) {
        BatchSendEmailDetailRespDto detailRespDto = new BatchSendEmailDetailRespDto();
        GroupTask groupTask = groupTaskRepository.findById(id).orElse(null);
        if (groupTask == null || !groupTask.getUserID().equals(Session.currentSession().getUserID())) {
            return null;
        }

        int page = 1;
        while (true) {
            PageResult<SubTask> subTaskPageResult = subTaskRepository.findByPagination(50000, page, new HashMap<>(Map.of("userID", Session.currentSession().userID, "groupTaskId", groupTask.get_id())), null);
            if (subTaskPageResult.getData().isEmpty()) {
                break;
            }
            for (SubTask subTask : subTaskPageResult.getData()) {
                if (subTask.getStatus().equals(SubTaskStatusEnums.success.getCode())) {
                    detailRespDto.setSuccessNum(detailRespDto.getSuccessNum() + 1);
                    if (subTask.getParams().getOrDefault("testABStep", "2").toString().equals("1")) {
                        if (subTask.getParams().getOrDefault("isB", "0").toString().equals("1")) {
                            detailRespDto.setSuccessNumB(detailRespDto.getSuccessNumB() + 1);
                        } else {
                            detailRespDto.setSuccessNumA(detailRespDto.getSuccessNumA() + 1);
                        }
                    }
                    if (subTask.getResult() != null) {
                        if (subTask.getResult().getOrDefault("open", "0").equals("1")) {
                            detailRespDto.setOpenNum(detailRespDto.getOpenNum() + 1);
                            if (subTask.getParams().getOrDefault("testABStep", "2").toString().equals("1")) {
                                if (subTask.getParams().getOrDefault("isB", "0").toString().equals("1")) {
                                    detailRespDto.setOpenNumB(detailRespDto.getOpenNumB() + 1);
                                } else {
                                    detailRespDto.setOpenNumA(detailRespDto.getOpenNumA() + 1);
                                }
                            }
                        }

                        if (subTask.getResult().getOrDefault("click", "0").equals("1")) {
                            detailRespDto.setClickNum(detailRespDto.getClickNum() + 1);
                            if (subTask.getParams().getOrDefault("testABStep", "2").toString().equals("1")) {
                                if (subTask.getParams().getOrDefault("isB", "0").toString().equals("1")) {
                                    detailRespDto.setClickNumB(detailRespDto.getClickNumB() + 1);
                                } else {
                                    detailRespDto.setClickNumA(detailRespDto.getClickNumA() + 1);
                                }
                            }
                        }

                        if (subTask.getResult().getOrDefault("reply", "0").equals("2")) {
                            detailRespDto.setCallbackNum(detailRespDto.getCallbackNum() + 1);
                        }

                        if (subTask.getResult().getOrDefault("reply", "0").equals("1")) {
                            if (subTask.getResult().getOrDefault("msg", "").toString().equals("退信")) {
                                detailRespDto.setCallbackNum(detailRespDto.getCallbackNum() + 1);
                            } else {
                                detailRespDto.setReplyNum(detailRespDto.getReplyNum() + 1);
                                if (subTask.getParams().getOrDefault("testABStep", "2").toString().equals("1")) {
                                    if (subTask.getParams().getOrDefault("isB", "0").toString().equals("1")) {
                                        detailRespDto.setReplyNumB(detailRespDto.getReplyNumB() + 1);
                                    } else {
                                        detailRespDto.setReplyNumA(detailRespDto.getReplyNumA() + 1);
                                    }
                                }
                            }
                        }
                    }
                }
                if (subTask.getStatus().equals(SubTaskStatusEnums.failed.getCode())) {
                    detailRespDto.setFailNum(detailRespDto.getFailNum() + 1);
                    if (subTask.getParams().getOrDefault("testABStep", "2").toString().equals("1")) {
                        if (subTask.getParams().getOrDefault("isB", "0").toString().equals("1")) {
                            detailRespDto.setFailNumB(detailRespDto.getFailNumB() + 1);
                        } else {
                            detailRespDto.setFailNumA(detailRespDto.getFailNumA() + 1);
                        }
                    }
                }
            }
            page++;
        }

        detailRespDto.setTotalA(detailRespDto.getSuccessNumA() + detailRespDto.getFailNumA());
        detailRespDto.setTotalB(detailRespDto.getSuccessNumB() + detailRespDto.getFailNumB());

        groupTask.setIds(null);
        detailRespDto.setGroupTask(groupTask);
        return detailRespDto;
    }

    public PageResult<SubTask> detailList(String id, Integer pageSize, Integer pageNum, Params params) {
        return subTaskRepository.findByPagination(pageSize, pageNum, params.getFilters(), params.getSorter());
    }

    public void executeAB(String id, String type) {
        GroupTask groupTask = groupTaskRepository.findById(id).orElse(null);
        if (groupTask == null || !groupTask.getStatus().equals(GroupTaskStatusEnums.init.getCode()) || !groupTask.getPublishStatus().equals("success")) {
            return;
        }


        String content = ((Map)groupTask.getParams().get("reqDto")).get("content").toString();
        String title = ((Map)groupTask.getParams().get("reqDto")).get("title").toString();
        String contentB = ((Map)groupTask.getParams().get("reqDto")).getOrDefault("contentB", "").toString();
        String titleB = ((Map)groupTask.getParams().get("reqDto")).getOrDefault("titleB", "").toString();

        String testAB = ((Map)groupTask.getParams().get("reqDto")).get("testAB").toString();
        String testABStep = groupTask.getParams().getOrDefault("testABStep", "1").toString();

        if (testAB.equals("yes") && testABStep.equals("1")) {
            if (type.equals("A")) {
                groupTask.getParams().put("testABStep", "2");
                groupTask.getParams().put("content", content);
                groupTask.getParams().put("title", title);
            }
            if (type.equals("B")) {
                groupTask.getParams().put("testABStep", "2");
                groupTask.getParams().put("content", contentB);
                groupTask.getParams().put("title", titleB);
            }

            groupTask.setPublishStatus("init");
            groupTaskRepository.save(groupTask);
        }
    }

    public void checkReply(GroupTask groupTask) {
        try {
            int page = 1;
            while (true) {
                PageResult<SubTask> subTaskPageResult = subTaskRepository.findByPagination(1000, page, Map.of("groupTaskId", groupTask.get_id(), "result.reply", Map.of("$exists", false), "status", SubTaskStatusEnums.success.getCode()), null);
                if (subTaskPageResult.getData().isEmpty()) {
                    break;
                }
                for (SubTask subTask : subTaskPageResult.getData()) {
                    List<Message> messageList = messageRepository.findByAccIdAndSenderAndCreateTimeGatherThan(subTask.getAccid(), List.of("mailer-daemon@googlemail.com", ""), subTask.getCreateTime());
                    if (!messageList.isEmpty()) {
                        // 所有退信
                        List<String> list = messageList.stream().map(Message::getThread_id).toList();
                        // 退信对应的发送的信
                        List<Message> tui = messageRepository.findByThreadIdIn(list, subTask.getCreateTime());
                        if (tui.stream().anyMatch(e -> e.getReceivers() != null && e.getReceivers().contains(subTask.getParams().getOrDefault("email", "").toString()))) {
                            // You have reached a limit for sending mail. Your message was not sent
                            if (messageList.stream().anyMatch(e -> e.getSummary() != null && e.getSummary().contains("You have reached a limit for sending mail. Your message was not sent"))) {
                                // 账号限制
                                accountRepository.updateLimitSendEmail(subTask.getAccid());
                            }
                            if (messageList.stream().anyMatch(e -> e.getSummary() != null && e.getSummary().contains("Message blocked Your message to"))) {
                                // 账号限制
                                accountRepository.updateLimitSendEmail(subTask.getAccid());
                            }
                            if (messageList.stream().anyMatch(e -> e.getText() != null && e.getText().contains("This mailbox is disabled (554.30)"))) {
                                if (subTask.getResult() == null) {
                                    subTask.setResult(new HashMap<>());
                                }
                                subTask.getResult().put("reply", "2");
                                subTask.getResult().put("msg", "退信(This mailbox is disabled)");

                                subTaskRepository.save(subTask);
                            } else if (messageList.stream().anyMatch(e -> e.getSummary() != null &&
                                    (e.getSummary().contains("Address not found Your message wasn't delivered")
                                    || e.getSummary().contains("Recipient address rejected"))
                            )) {
                                if (subTask.getResult() == null) {
                                    subTask.setResult(new HashMap<>());
                                }
                                subTask.getResult().put("reply", "2");
                                subTask.getResult().put("msg", "退信(Address not found)");

                                subTaskRepository.save(subTask);
                            } else if (messageList.stream().anyMatch(e -> e.getSummary() != null && e.getSummary().contains("Recipient inbox full Your message couldn't be delivered"))) {
                                if (subTask.getResult() == null) {
                                    subTask.setResult(new HashMap<>());
                                }
                                subTask.getResult().put("reply", "2");
                                subTask.getResult().put("msg", "退信(Recipient inbox full)");

                                subTaskRepository.save(subTask);
                            } else {
                                Integer tuiRetryTotal = 5;
                                try {
                                    tuiRetryTotal = Integer.valueOf(paramsService.getParams("account.tuiRetry", null, null).toString());
                                } catch (Exception e) {}
                                if (subTask.getResult() == null) {
                                    subTask.setResult(new HashMap<>());
                                }
                                Integer tuiRetry = 0;
                                try {
                                    tuiRetry = Integer.valueOf(subTask.getParams().getOrDefault("tuiRetry", "0").toString());
                                } catch (Exception e) {}
                                if (tuiRetry >= tuiRetryTotal) {
                                    subTask.getResult().put("reply", "2");
                                    subTask.getResult().put("msg", "退信(Retry "+tuiRetryTotal+" times failed)");
                                } else {
                                    subTask.setAccid("");
                                    subTask.setStatus(SubTaskStatusEnums.processing.getCode());
                                    subTask.setFinishTime(null);
                                    subTask.setResult(null);
                                    subTask.getParams().put("tuiRetry", tuiRetry + 1);
                                }
                                subTaskRepository.save(subTask);
                                if (tuiRetry < tuiRetryTotal) {
                                    // 更新
                                    if (groupTask.getPublishStatus().equals("success")) {
                                        groupTaskRepository.updatePublishStatus(groupTask.get_id(), "init", GroupTaskStatusEnums.init.getCode());
                                    }
                                }
                            }
                        }
                    }

                    messageList = messageRepository.findByAccIdAndSenderAndCreateTimeGatherThan(subTask.getAccid(), List.of(subTask.getParams().getOrDefault("email", "").toString()), subTask.getCreateTime());
                    if (!messageList.isEmpty()) {
                        if (subTask.getResult() == null) {
                            subTask.setResult(new HashMap<>());
                        }
                        subTask.getResult().put("reply", "1");
                        subTaskRepository.save(subTask);

                        SendEmailEventMonitor monitor = sendEmailEventMonitorRepository.findOneByUUID(subTask.getTmpId(), "reply");
                        if (monitor != null) {
                            saveTracking(monitor, Map.of("replyText", messageList.getFirst().getText(), "replySubject", messageList.getFirst().getSubject()));
                        }
                    }
                }
                page++;
            }
        } catch (Exception e) {
            log.info("checkReply ERROR:" + e.getMessage());
        }
    }

    private void saveTracking(SendEmailEventMonitor one, Map<String, String> map) {
        try {
            if (one.getCount() == null) {
                one.setCount(0);
            }
            one.setCount(one.getCount() + 1);
            sendEmailEventMonitorRepository.update(one);

            SendEmailEventTracking sendEmailEventTracking = new SendEmailEventTracking();
            sendEmailEventTracking.setEvent(one.getEvent());
            sendEmailEventTracking.setEmail(one.getEmail());
            sendEmailEventTracking.setCreateTime(new Date());
            sendEmailEventTracking.setParams(map);
            sendEmailEventTracking.setGroupTaskId(one.getGroupTaskId());
            sendEmailEventTracking.setSubTaskId(one.getSubTaskId());
            sendEmailEventTracking.setUserID(one.getUserID());

            sendEmailEventTrackingRepository.save(sendEmailEventTracking);
        } catch (Exception e) {}
    }

    public GroupTask testV2(BatchSendEmailTestReqDto reqDTO, String userID) {
        // 判断余额
        reqDTO.setTaskName("检测邮箱限制任务");
        if (CollectionUtils.isEmpty(reqDTO.getIds())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }

        List<Account> accountList = accountRepository.findByIds(reqDTO.getIds());

        reqDTO.setIds(accountList.stream().filter(Account::getLimitSendEmail).map(Account::get_id).toList());

        if (reqDTO.getIds().isEmpty()) {
            return null;
        }

        String accountUserID = userID;
        Set<MenuType> permissionSet = userService.getPermissionSet(userID);
        if (!permissionSet.contains(MenuType.accountGroup)) {
            accountUserID = Constants.ADMIN_USER_ID;
        }

        String sendEmailMaxNumByDay = paramsService.getParams("account.sendEmailMaxNumByDay", null, null).toString();
        // 所有邮箱
        AccountListDTO accountListDTO = new AccountListDTO();
        accountListDTO.setFilters(new HashMap<>(Map.of("limitSendEmail", Map.of("$ne", true), "userID", accountUserID,
                "onlineStatus", AccountOnlineStatus.ONLINE.getCode(), "isCheck", true,
                "sendEmailNumByDay", Map.of("$lt", Integer.parseInt(sendEmailMaxNumByDay)))));
        AccountGroup group = accountGroupRepository.findByGroupNameAndUserID("群发邮件分组", accountUserID);
        if (group != null) {
            accountListDTO.getFilters().put("groupID", group.get_id());
        }
        accountListDTO.getFilters().put("type", Map.of("$ne",AccountTypeEnums.sendgrid.getCode()));
        accountListDTO.setSorter(new HashMap<>(Map.of("lastPullMessageTime", -1)));
        PageResult<Account> accountPageResult = accountRepository.findByPagination(accountListDTO, reqDTO.getIds().size(), 1);
        if (accountPageResult.getData().size() != reqDTO.getIds().size()) {
            throw new CommonException(ResultCode.NO_ENOUGH_ACCOUNT);
        }

        reqDTO.setAddData(String.join("\n", accountPageResult.getData().stream().map(Account::getEmail).toList()));
        String sendEmailIntervalInSecond = paramsService.getParams("account.sendEmailIntervalInSecond", null, null).toString();

        GroupTask groupTask = taskUtil.createGroupTask(reqDTO.getIds(), TaskTypesEnums.BatchSendEmailTestV2, new HashMap(Map.of(
                "accountUserID", accountUserID,
                "sendEmailIntervalInSecond", sendEmailIntervalInSecond,
                "taskDesc", "[测试]" + (reqDTO.getTaskName() == null ? "" : reqDTO.getTaskName()),
                "publishTotalCount", reqDTO.getIds().size(),
                "addDatas", reqDTO.getAddData() == null ? "" : Arrays.stream(reqDTO.getAddData().split("\n")).toList(),
                "addMethod", "1",
                "filepath", "",
                "reqDto", reqDTO)), userID, "1", null);

        return groupTask;
    }
}
