package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.model.dto.AccountPlatformReqDto;
import com.nyy.gmail.cloud.repository.mongo.AccountPlatformRepository;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.PlatformPriceRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AccountPlatformService {

    @Autowired
    private AccountPlatformRepository accountPlatformRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PlatformPriceRepository platformPriceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformPriceService platformPriceService;

    public void save(AccountPlatform accountPlatform, String userID) {
        if (StringUtils.isEmpty(accountPlatform.getName())) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (StringUtils.isNotEmpty(accountPlatform.get_id())) {
            AccountPlatform update = accountPlatformRepository.findOneByIdAndUserID(accountPlatform.get_id(), userID);
//            if (update.getPrice().compareTo(accountPlatform.getPrice()) < 0) {
//                // 小改大
//                platformPriceService.processPriceUpdate(List.of(Constants.ADMIN_USER_ID), accountPlatform.get_id(), accountPlatform.getPrice());
//            }
            update.setName(accountPlatform.getName());
            update.setPattern(accountPlatform.getPattern());
            update.setEmailFrom(accountPlatform.getEmailFrom());
            update.setSortNo(accountPlatform.getSortNo());
            update.setPrice(accountPlatform.getPrice());
            update.setIcon(accountPlatform.getIcon());
            update.setDisplayStatus(accountPlatform.getDisplayStatus());
            accountPlatformRepository.update(update);
        } else {
            AccountPlatform newAcc = new AccountPlatform();
            newAcc.setCanUseAccountNumber(0);
            newAcc.setEmailFrom(accountPlatform.getEmailFrom());
            newAcc.setName(accountPlatform.getName());
            newAcc.setUserID(userID);
            newAcc.setPattern(accountPlatform.getPattern());
            newAcc.setCreateTime(new Date());
            newAcc.setSortNo(accountPlatform.getSortNo());
            newAcc.setIcon(accountPlatform.getIcon());
            newAcc.setPrice(accountPlatform.getPrice());
            newAcc.setDisplayStatus(accountPlatform.getDisplayStatus());
            accountPlatformRepository.save(newAcc);
        }
    }

    public void delete(List<String> ids, String userID) {
        List<AccountPlatform> platforms = accountPlatformRepository.findByIdIn(ids, userID);
        for (AccountPlatform platform : platforms) {
            accountPlatformRepository.delete(platform);
        }
    }

    public PageResult<AccountPlatform> list(AccountPlatformReqDto data, String userID, int pageSize, int page) {
        PageResult<AccountPlatform> pagination = accountPlatformRepository.findByPagination(data, pageSize, page);
        return pagination;
    }

    public void updateStock (String userID) {
        List<AccountPlatform> platforms = accountPlatformRepository.findByUserID(userID);
        long count = accountRepository.countByUserIDAndOnlineStatus(userID, AccountOnlineStatus.ONLINE.getCode());

        for (AccountPlatform platform : platforms) {
            long used = accountRepository.countByPlatformIdAndOnlineStatus(platform.get_id(), userID, AccountOnlineStatus.ONLINE.getCode());
            platform.setCanUseAccountNumber((int)(count - used));
            accountPlatformRepository.update(platform);
        }
    }

    public List<AccountPlatform> findAll(String userID) {
        return accountPlatformRepository.findByUserID(userID);
    }

    public int saveBatch(List<AccountPlatform> entities) {
        return accountPlatformRepository.saveBatch(entities);
    }

    public AccountPlatform findPriceById(String platformId, String userID) {
        AccountPlatform accountPlatform = accountPlatformRepository.findOneByIdAndUserID(platformId, Constants.ADMIN_USER_ID);
        if (accountPlatform == null) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }

        PlatformPrice price = platformPriceRepository.findOneByUserIDAndPlatformId(userID, platformId);
        if (price == null || price.getPrice() == null || price.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            accountPlatform.setPrice(accountPlatform.getPrice().compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : accountPlatform.getPrice());

            User one = userRepository.findOneByUserID(userID);

            List<String> userIDs = Arrays.stream(one.getReferrer().split(",")).toList();
            List<PlatformPrice> platformPriceList = platformPriceRepository.findByUserIDs(userIDs);

            Map<String, Map<String, BigDecimal>> map = new HashMap<>();
            for (PlatformPrice platformPrice : platformPriceList) {
                if (platformPrice.getPlatformId().equals(platformId)) {
                    if (map.containsKey(platformPrice.getUserID())) {
                        map.get(platformPrice.getUserID()).put(platformPrice.getPlatformId(), platformPrice.getPrice());
                    } else {
                        map.put(platformPrice.getUserID(), new HashMap<>());
                        map.get(platformPrice.getUserID()).put(platformPrice.getPlatformId(), platformPrice.getPrice());
                    }
                }
            }
            for (int i = userIDs.size() - 1; i >= 0; i--) {
                if (map.containsKey(userIDs.get(i))) {
                    if (map.get(userIDs.get(i)).containsKey(platformId)) {
                        accountPlatform.setPrice(map.get(userIDs.get(i)).get(platformId));
                        break;
                    }
                }
            }
        } else {
            accountPlatform.setPrice(price.getPrice());
        }
        return accountPlatform;
    }
}
