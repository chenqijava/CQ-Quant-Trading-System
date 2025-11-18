package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.JpaPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.AccountGroup;
import com.nyy.gmail.cloud.enums.AccountGroupTypeEnums;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.model.dto.AccountGroupListDTO;
import com.nyy.gmail.cloud.model.dto.AccountGroupSaveDTO;
import com.nyy.gmail.cloud.model.dto.GroupAccountStats;
import com.nyy.gmail.cloud.repository.mongo.AccountGroupRepository;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.Socks5Repository;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AccountGroupService {

    @Resource
    private AccountGroupRepository accountGroupRepository;

    @Resource
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

    public AccountGroupSaveDTO save(AccountGroup accountGroup, String userID, AccountGroupTypeEnums accountGroupTypeEnums) {
        // 校验参数
        if (accountGroup == null) {
            throw new CommonException("accountGroup is null");
        }
        if (accountGroup.getGroupName() == null || accountGroup.getGroupName().isEmpty()) {
            throw new CommonException("groupName is null");
        }

        // 新增
        if (StringUtil.isEmpty(accountGroup.get_id())) {
            // 根据groupName查询AccountGroup
            AccountGroup resAccountGroup = accountGroupRepository.findByGroupNameAndUserID(accountGroup.getGroupName(), userID);
            if (null != resAccountGroup) {
                throw new CommonException("分组名称不能重复");
            }
            accountGroup.setVersion(0L);
            accountGroup.setUserID(userID);
            accountGroup.setCreateTime(new Date());
            accountGroup.setGroupType(accountGroupTypeEnums.getCode());
            accountGroupRepository.save(accountGroup);
        } else {
            AccountGroup updateAccountGroup = accountGroupRepository.findById(accountGroup.get_id());
            // 修改
            updateAccountGroup.setGroupName(accountGroup.getGroupName());
            accountGroupRepository.updateGroupName(accountGroup.get_id(), accountGroup.getGroupName());
        }
        return new AccountGroupSaveDTO();
    }

    public PageResult<AccountGroup> findAccountByPagination(AccountGroupListDTO accountGroupListDTO, String userID, int pageSize, int page) {
        PageResult<AccountGroup> pagination = accountGroupRepository.findByPagination(accountGroupListDTO, pageSize, page);
        List<AccountGroup> accountGroupList = pagination.getData();
        if (CollectionUtils.isEmpty(accountGroupList)) {
            return pagination;
        }

        if (accountGroupListDTO.isIncludeAccountNum()) {
            List<String> groupIdList = accountGroupList.stream().map(AccountGroup::get_id).toList();
            List<GroupAccountStats> groupAccountStats = accountRepository.aggregateAccountStats(groupIdList, userID);
            Map<String, GroupAccountStats> collect = groupAccountStats.stream().collect(Collectors.toMap(GroupAccountStats::getId, e -> e, (k1, k2) -> k1));
            for (AccountGroup accountGroup : accountGroupList) {
                GroupAccountStats count = collect.get(accountGroup.get_id());
                if (count != null) {
                    accountGroup.setAccountNum(count.getTotal());
                    accountGroup.setAccountOnlineNum(count.getOnlineTotal());
                } else {
                    accountGroup.setAccountNum(0L);
                    accountGroup.setAccountOnlineNum(0L);
                }
            }

//            // 查询Account
//            List<Account> byGroupIdListAndUserIDAndHasLoginSuccess = accountRepository.findByGroupIdListAndUserID(groupIdList, userID);
//            // 根据groupId做分组，并统计sum
//            Map<String, Long> groupByGroupIdMap = byGroupIdListAndUserIDAndHasLoginSuccess.stream().collect(Collectors.groupingBy(Account::getGroupID, Collectors.counting()));
//            for (AccountGroup accountGroup : accountGroupList) {
//                Long count = groupByGroupIdMap.get(accountGroup.get_id());
//                if (count == null) {
//                    count = 0L;
//                }
//                accountGroup.setAccountNum(count);
//            }
//
//            // 过滤出来在线的
//            List<Account> onlineAccountList = byGroupIdListAndUserIDAndHasLoginSuccess.stream().filter(x -> AccountOnlineStatus.ONLINE.getCode().equals(x.getOnlineStatus())).toList();
//            // 根据groupId做分组，并统计sum
//            Map<String, Long> onlineGroupByGroupIdMap = onlineAccountList.stream().collect(Collectors.groupingBy(Account::getGroupID, Collectors.counting()));
//            for (AccountGroup accountGroup : accountGroupList) {
//
//                Long count = onlineGroupByGroupIdMap.get(accountGroup.get_id());
//                if (count == null) {
//                    count = 0L;
//                }
//                accountGroup.setAccountOnlineNum(count);
//            }
        }
        return pagination;
    }

    public void delete(List<String> ids, String userID) {

        // 根绝ids查询分组信息
        List<AccountGroup> byIdListAndUserID = accountGroupRepository.findByIdListAndUserID(ids, userID);
        // 过滤出来是否包含默认的分组
        List<AccountGroup> defaultGroup = byIdListAndUserID.stream().filter(x -> AccountGroupTypeEnums.DEFAULT.getCode().equals(x.getGroupType())).toList();
        if (!CollectionUtils.isEmpty(defaultGroup)) {
            throw new CommonException("不能删除默认分组");
        }

        Long count = accountRepository.countByGroupIdListAndUserID(ids, userID);
        if (count > 0) {
            throw new CommonException("已选分组下有账号，不能删除");
        }

        for (String id : ids) {
            AccountGroup accountGroup = accountGroupRepository.findByIdAndUserID(id, userID);
            if (accountGroup != null) {
                accountGroupRepository.deleteManyByIds(List.of(id));
            }
        }
    }
}
