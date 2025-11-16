package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.JpaPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.gateway.GatewayClient;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.repository.mysql.VpsInfoRepository;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AccountService {

    @Autowired
    private GatewayClient gatewayClient;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private Socks5Repository socks5Repository;

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private JpaPaginationHelper jpaPaginationHelper;

    @Resource
    private VpsInfoRepository vpsInfoRepository;

    @Autowired
    private TaskUtil taskUtil;

    @Resource
    private AccountGroupRepository accountGroupRepository;

    @Resource
    private AccountServiceHelper accountServiceHelper;

    @Autowired
    private AccountPlatformRepository accountPlatformRepository;

    @Autowired
    private AccountExportRecordRepository accountExportRecordRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    public PageResult<Account> findAccountByPagination(AccountListDTO accountListDTO, int pageSize, int page) {
        PageResult<Account> pagination = accountRepository.findByPagination(accountListDTO, pageSize, page);
        return pagination;
    }

    public void delete(List<String> ids, String userID, boolean needCheckOnlineStatus) {
        if (needCheckOnlineStatus) {
            List<String> onlineStatusList; onlineStatusList = Collections.singletonList(AccountOnlineStatus.ONLINE.getCode());
            List<Account> accounts = accountRepository.findByIdsInAndOnlineStatusInAndUserID(ids, onlineStatusList, userID);
            if (!CollectionUtils.isEmpty(accounts)) {
                throw new CommonException(ResultCode.DELETE_ACCOUNT_FAIL);
            }
        }

        for (String id : ids) {
            Account account = accountRepository.findByIdAndUserID(id, userID);
            if (account != null) {
                socks5Repository.findAndRelease(account.get_id());
                accountRepository.deleteManyByIds(List.of(id));
            }
        }
    }

    public boolean canDeleteUser(String userID) {
        List<String> status = Arrays.asList(AccountOnlineStatus.WAITING_ONLINE.getCode(), AccountOnlineStatus.ONLINE.getCode());
        List<Account> accounts = accountRepository.findByOnlineStatus(status, userID);
        return accounts.size() <= 0;
    }

    public void deleteByUserID(String userID) {
        accountRepository.deleteByUserID(userID);
    }

    public Account getOneById(String accid) {
        // 加缓存
        String key = "AccountCache:" + accid;
        RBucket<Account> bucket = redissonClient.getBucket(key);
        Account account = bucket.get();

        if (account == null) {
            account = accountRepository.findById(accid);
            long expireTime = 30;
            bucket.set(account, expireTime, TimeUnit.SECONDS);
        }
        return account;
    }

    public void editAccount(EditAccountDTO editAccountDTO, String userID) {
        if (StringUtils.isBlank(editAccountDTO.get_id())) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }

        if (StringUtils.isBlank(editAccountDTO.getGroupID())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }

        // 根据accountId查询数据
        Account account = accountRepository.findByIdAndUserID(editAccountDTO.get_id(), userID);
        if (null == account) {
            throw new CommonException(ResultCode.NO_ACCOUNT);
        }

        Query query = new Query(Criteria.where("_id").is(editAccountDTO.get_id()));
        Update update = new Update();
        update.set("groupID", editAccountDTO.getGroupID());
        update.set("remark", editAccountDTO.getRemark());
        mongoTemplate.updateFirst(query, update, Account.class);

    }

    public int getUnbindDeviceCount(String userID) {
        return vpsInfoRepository.countByUserIdAndBindStatusAndDeadTimeGreaterThan(userID, "0", new Date());
    }

    public void editGroup(EditAccountGroupDTO editAccountGroupDTO, String userID) {
        if (CollectionUtils.isEmpty(editAccountGroupDTO.getIds())) {
            throw new CommonException(ResultCode.PARAMS_IS_BLANK);
        }

        if (StringUtils.isBlank(editAccountGroupDTO.getGroupID())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }

        for (String id : editAccountGroupDTO.getIds()) {
            Query query = new Query(Criteria.where("_id").in(id).and("userID").is(userID));
            Update update = new Update();
            update.set("groupID", editAccountGroupDTO.getGroupID());
            mongoTemplate.updateFirst(query, update, Account.class);
        }
    }

    public StatisticsAccountDTO statisticsAccount(String userID) {
        // 总数
        long total = mongoTemplate.count(
                Query.query(Criteria.where("userID").is(userID)),
                Account.class
        );

        // 在线的
        long onlineTotal = mongoTemplate.count(
                Query.query(Criteria.where("userID").is(userID)
                        .and("onlineStatus").is(AccountOnlineStatus.ONLINE.getCode())),
                Account.class
        );

        // 离线的
        long offlineTotal = mongoTemplate.count(
                Query.query(Criteria.where("userID").is(userID)
                        .and("onlineStatus").is(AccountOnlineStatus.OFFLINE.getCode())),
                Account.class
        );

        // 封禁的
        long bannedTotal = mongoTemplate.count(
                Query.query(Criteria.where("userID").is(userID)
                        .and("onlineStatus").is(AccountOnlineStatus.BANNED.getCode())),
                Account.class
        );

        return new StatisticsAccountDTO(total, onlineTotal, offlineTotal, bannedTotal);
    }

    public void importAccount(ImportAccountReqDto reqDto, String userID) {
        if (StringUtils.isEmpty(reqDto.getFilepath())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        Path resPath = FileUtils.resPath;
        Path path = resPath.resolve(reqDto.getFilepath()).toAbsolutePath().normalize();
        int i = FileUtils.readCsvFileLineCount(path.toString());

        taskUtil.createGroupTask(new ArrayList<>(), TaskTypesEnums.AccountImport, Map.of(
                "openExportReceiveCode", reqDto.getOpenExportReceiveCode(),
                "groupId", reqDto.getGroupId(),
                "type", reqDto.getType(),
                "filepath", reqDto.getFilepath(),
                "publishTotalCount", (long) i,
                "taskDesc", "导入账号",
                "addMethod", "2"), userID);
    }

    public GroupTask exportAccount(ExportAccountReqDto reqDto, String userID) {
        if (StringUtils.isEmpty(reqDto.getPlatformId())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (reqDto.getCount() == null && CollectionUtils.isEmpty(reqDto.getIds())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }

        AccountPlatform platform = accountPlatformRepository.findOneByIdAndUserID(reqDto.getPlatformId(), userID);

        int count = CollectionUtils.isEmpty(reqDto.getIds()) ? reqDto.getCount() : reqDto.getIds().size();
        AccountExportRecord record = new AccountExportRecord();
        record.setNumber(count);
        record.setCreateTime(new Date());
        record.setUserID(userID);
        record.setPlatformId(reqDto.getPlatformId());
        record.setPlatformName(platform.getName());

        accountExportRecordRepository.save(record);
        // 创建记录
        return taskUtil.createGroupTask(new ArrayList<>(), TaskTypesEnums.AccountExport, Map.of(
                "orderId", reqDto.getOrderId() == null ? "" : reqDto.getOrderId(),
                "exportType", reqDto.getExportType(),
                "taskDesc", "导出账号",
                "publishTotalCount", count,
                "platformId", reqDto.getPlatformId(),
                "count", reqDto.getCount() == null ? 0 : reqDto.getCount(),
                "recordId", record.get_id(),
                "ids", reqDto.getIds() == null ? new ArrayList<>() : reqDto.getIds()), userID);
    }

    public PageResult<AccountExportRecord> exportAccountList(ExportAccountListReqDto reqDto, int pageSize, int page, String userID) {
        reqDto.getFilters().put("userID", userID);
        return accountExportRecordRepository.findByPagination(reqDto, pageSize, page);
    }

    public PageResult<UsedInfoRespDto> usedInfoList(UsedInfoReqDto reqDto, int pageSize, int page, String userID) {
        List<AccountPlatform> platforms = accountPlatformRepository.findByUserID(userID);
        Account account = accountRepository.findByIdAndUserID(reqDto.getAccId(), userID);
        if (account == null) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        List<SubTask> subTaskList = subTaskRepository.findByAccIdAndType(account.get_id(), TaskTypesEnums.AccountExport.getCode());
        Map<String, Date> usedTimeMap = new HashMap<>();
        Map<String, Date> realUsedTimeMap = new HashMap<>();
        subTaskList.forEach(e -> {
            if (e.getParams() != null && e.getParams().containsKey("platformId")) {
                usedTimeMap.put(e.getParams().get("platformId").toString(), e.getCreateTime());
                if (e.getParams() != null && e.getParams().containsKey("realUsedTime")) {
                    realUsedTimeMap.put(e.getParams().get("platformId").toString(), (Date) e.getParams().get("realUsedTime"));
                }

            }
        });
        List<UsedInfoRespDto> list = platforms.stream().map(e -> {
            UsedInfoRespDto usedInfoRespDto = new UsedInfoRespDto();
            if (usedTimeMap.containsKey(e.get_id())) {
                usedInfoRespDto.setUsedTime(usedTimeMap.get(e.get_id()));
                usedInfoRespDto.setUsed(true);
            } else {
                usedInfoRespDto.setUsed(false);
            }
            if (realUsedTimeMap.containsKey(e.get_id())) {
                usedInfoRespDto.setRealUsedTime(realUsedTimeMap.get(e.get_id()));
            }
            if (account.getRealUsedPlatformIds() != null && account.getRealUsedPlatformIds().contains(e.get_id())) {
                usedInfoRespDto.setRealUsed(true);
            } else {
                usedInfoRespDto.setRealUsed(false);
            }
            usedInfoRespDto.setPlatformId(e.get_id());
            usedInfoRespDto.setPlatformName(e.getName());
            return usedInfoRespDto;
        }).toList();

        if (StringUtils.isNotEmpty(reqDto.getPlatformName())) {
            list = list.stream().filter(e -> e.getPlatformName().contains(reqDto.getPlatformName())).toList();
        }

        if (reqDto.getUsed() != null) {
            list = list.stream().filter(e -> e.getUsed().equals(reqDto.getUsed())).toList();
        }

        PageResult<UsedInfoRespDto> pageResult = new PageResult<>();
        pageResult.setPages(list.size() / pageSize + 1);
        pageResult.setPageNum(page);
        pageResult.setPageSize(pageSize);
        pageResult.setTotal((long)list.size());

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, list.size());

        if (fromIndex >= list.size() || fromIndex < 0) {
            pageResult.setData(Collections.emptyList());
        } else {
            pageResult.setData(list.subList(fromIndex, toIndex));
        }
        return pageResult;
    }

    public void resetPlatform(ResetPlatformReqDto reqDto, String userID) {
        Account account = accountRepository.findById(reqDto.getAccId());
        if (account == null) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (account.getUsedPlatformIds() != null && account.getUsedPlatformIds().contains(reqDto.getPlatformId())) {
            account.setUsed(account.getUsed() == null ? 0 : account.getUsed() <= 0 ? 0 : account.getUsed() - 1);
            account.setUsedPlatformIds(account.getUsedPlatformIds().stream().filter(e -> !e.equals(reqDto.getPlatformId())).toList());
            if (account.getRealUsedPlatformIds() != null) {
                account.setRealUsedPlatformIds(account.getRealUsedPlatformIds().stream().filter(e -> !e.equals(reqDto.getPlatformId())).toList());
            }
            accountRepository.update(account);
        }

        List<SubTask> subTaskList = subTaskRepository.findByAccIdAndType(account.get_id(), TaskTypesEnums.AccountExport.getCode());
        subTaskList.forEach(e -> {
            if (e.getParams() != null && e.getParams().containsKey("platformId")) {
                if (e.getParams().get("platformId").equals(reqDto.getPlatformId())) {
                    e.setAccid("");
                    subTaskRepository.save(e);
                }
            }
        });
    }

    public Integer queryPlatformCount(QueryPlatformCountReqDto reqDto, String userID) {
        return (int) accountRepository.countNoUsePlatformStock(reqDto.getIds(), reqDto.getPlatformId(), userID);
    }

    public List<Account> findAllIncludePart(String userID, AccountListDTO accountListDTO) {
        return accountRepository.findAllIncludePart(userID, accountListDTO);
    }

    public void retryCheck(IdsListDTO reqDto, String userID) {
        for (String id : reqDto.getIds()) {
            Account account = accountRepository.findByIdAndUserID(id, userID);
            if (account != null) {
                account.setIsCheck(false);
                account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                account.setRetryCheckNum(account.getRetryCheckNum() + 1);
                account.setChangeOnlineStatusTime(new Date());
                accountRepository.update(account);
            }
        }
    }

    public void setGroup(Map reqDto, String userID) {
        List<String> ids = (List<String>) reqDto.get("ids");
        String groupId = (String) reqDto.get("groupId");
        int count =  (int) reqDto.get("count");
        if (ids == null) {
            Map filters = (Map)reqDto.get("filters");
            AccountListDTO accountListDTO = new AccountListDTO();
            accountListDTO.setFilters(filters);
            accountListDTO.setSorter(Map.of("_id", 1));
            PageResult<Account> accountPageResult = accountRepository.findByPagination(accountListDTO, count <= 0 ? 100000 : count, 1);
            accountRepository.updateSetGroup(accountPageResult.getData().stream().map(Account::get_id).toList(), userID,groupId);
        } else {
            for (String id : ids) {
                Account account = accountRepository.findByIdAndUserID(id, userID);
                if (account != null) {
                    account.setGroupID(groupId);
                    accountRepository.update(account);
                }
            }
        }
    }

    public void saveSendGrid(SendGridAccountDto sendGridAccountDto) {
        Account account = new Account();
        List<Account> accounts = accountRepository.findByApiKeysIn(List.of(sendGridAccountDto.getApiKey()));
        accounts = accounts.stream().filter(e -> e.getEmail().equals(sendGridAccountDto.getEmail())).toList();
        if (!accounts.isEmpty()) {
            throw new CommonException(ResultCode.DATA_IS_EXISTED);
        }
        account.setType(AccountTypeEnums.sendgrid.getCode());
        account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
        account.setChangeOnlineStatusTime(new Date());
        account.setAccID(sendGridAccountDto.getEmail());
        account.setEmail(sendGridAccountDto.getEmail());
        account.setSendGridApiKey(sendGridAccountDto.getApiKey());
        account.setUserID(sendGridAccountDto.getUserID());
        account.setIsCheck(true);
        account.setCreateTime(new Date());
        accountRepository.save(account);
    }

    public int saveBatch(List<Account> entities) {
        List<String> apiKeys = entities.stream().map(Account::getSendGridApiKey).toList();
        List<Account> keys = accountRepository.findByApiKeysIn(apiKeys);
//        List<String> exists = keys.stream().map(Account::getSendGridApiKey).toList();
        // 去掉存在的保存
        entities = entities.stream().filter(e -> keys.stream().noneMatch(a -> a.getEmail().equals(e.getEmail()) && a.getSendGridApiKey().equals(e.getSendGridApiKey()))).toList();
        return accountRepository.saveBatch(entities);
    }

    public void saveWorkspace(WorkspaceAccountDto workspaceAccountDto) {
        if (AccountTypeEnums.workspace_service_account.getCode().equals(workspaceAccountDto.getType())) {
            String jsonFilePath =  workspaceAccountDto.getJsonFilePath();
            String emailFilePath = workspaceAccountDto.getEmailFilePath();
            if (StringUtils.isBlank(jsonFilePath) || StringUtils.isBlank(emailFilePath)) {
                return;
            }
            Path resPath = FileUtils.resPath;
            Path emailFileResPath = resPath.resolve(emailFilePath).toAbsolutePath().normalize();
            List<String[]> emails = FileUtils.readCsv(emailFileResPath);
            emails = emails.subList(1, emails.size());
            List<Account> addAccounts = new ArrayList<>();
            for (String[] email : emails) {
                if (StringUtils.isBlank(email[0])) {
                    continue;
                }
                Account account = new Account();
                List<Account> accounts = accountRepository.findbyEmails(List.of(email[0]));
                accounts = accounts.stream().filter(e -> e.getEmail().equals(email[0])).toList();
                if (!accounts.isEmpty()) {
                    throw new CommonException(ResultCode.DATA_IS_EXISTED);
                }
                account.setType(AccountTypeEnums.workspace_service_account.getCode());
                account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
                account.setChangeOnlineStatusTime(new Date());
                account.setAccID(email[0]);
                account.setEmail(email[0]);
                account.setWorkspaceCredentialJSON(jsonFilePath);
                account.setUserID(workspaceAccountDto.getUserID());
                account.setIsCheck(true);
                account.setCreateTime(new Date());
                account.setGroupID(workspaceAccountDto.getGroupId());
                addAccounts.add(account);
            }
            accountRepository.saveBatch(addAccounts);
        } else if (AccountTypeEnums.workspace_second_hand_account.getCode().equals(workspaceAccountDto.getType())) {
            if (StringUtils.isBlank(workspaceAccountDto.getEmail()) ||
                    StringUtils.isBlank(workspaceAccountDto.getClientId()) ||
                    StringUtils.isBlank(workspaceAccountDto.getClientSecret()) ||
                    StringUtils.isBlank(workspaceAccountDto.getRefreshToken())) {
                return;
            }
            Account account = new Account();
            List<Account> accounts = accountRepository.findbyEmails(List.of(workspaceAccountDto.getEmail()));
            accounts = accounts.stream().filter(e -> e.getEmail().equals(workspaceAccountDto.getEmail())).toList();
            if (!accounts.isEmpty()) {
                throw new CommonException(ResultCode.DATA_IS_EXISTED);
            }
            account.setType(AccountTypeEnums.workspace_second_hand_account.getCode());
            account.setOnlineStatus(AccountOnlineStatus.ONLINE.getCode());
            account.setChangeOnlineStatusTime(new Date());
            account.setAccID(workspaceAccountDto.getEmail());
            account.setEmail(workspaceAccountDto.getEmail());
            account.setWorkspaceClientId(workspaceAccountDto.getClientId());
            account.setWorkspaceClientSecret(workspaceAccountDto.getClientSecret());
            account.setWorkspaceRefreshToken(workspaceAccountDto.getRefreshToken());
            account.setUserID(workspaceAccountDto.getUserID());
            account.setIsCheck(true);
            account.setCreateTime(new Date());
            account.setGroupID(workspaceAccountDto.getGroupId());
            accountRepository.save(account);
        }
    }

    public void saveYahoo(ImportAccountReqDto reqDto) {
        String userID = Session.currentSession().getUserID();
        if (StringUtils.isEmpty(reqDto.getFilepath())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        Path resPath = FileUtils.resPath;
        Path path = resPath.resolve(reqDto.getFilepath()).toAbsolutePath().normalize();
        List<String[]> lines = FileUtils.readCsv(path);
        Integer count = 0;
        List<String> addDatas = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        for (String[] line: lines) {
            if (line[0].startsWith("– Email")) {
                String email = line[0].substring(9);
                if (!email.contains("@")) {
                    email = email + "@yahoo.com";
                }
                sb = new StringBuffer(email).append("----").append(sb);
            }
            if (line[0].equals("----")) {
                count++;
                addDatas.add(sb.toString());
                sb = new StringBuffer();
            } else {
                sb.append(line[0]).append("\n");
            }
        }
        addDatas.add(sb.toString());
        taskUtil.createGroupTask(new ArrayList<>(), TaskTypesEnums.AccountImport, Map.of(
                "openExportReceiveCode", reqDto.getOpenExportReceiveCode(),
                "groupId", reqDto.getGroupId(),
                "type", reqDto.getType(),
                "addDatas", addDatas,
                "publishTotalCount", (long) count,
                "taskDesc", "导入账号",
                "addMethod", "1"), userID);

    }
}
