package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.entity.mongo.AccountPlatform;
import com.nyy.gmail.cloud.entity.mongo.PlatformPrice;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.model.dto.PlatformPriceRespDto;
import com.nyy.gmail.cloud.repository.mongo.AccountPlatformRepository;
import com.nyy.gmail.cloud.repository.mongo.PlatformPriceRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlatformPriceService {

    @Autowired
    private PlatformPriceRepository platformPriceRepository;

    @Autowired
    private AccountPlatformRepository accountPlatformRepository;

    @Autowired
    private UserRepository userRepository;


    public List<PlatformPriceRespDto> queryPrice(String id) {
        List<AccountPlatform> accountPlatforms = accountPlatformRepository.findByUserID(Constants.ADMIN_USER_ID);

        List<PlatformPrice> platformPrices = platformPriceRepository.findByUserID(id);

        Map<String, PlatformPrice> priceMap = platformPrices.stream().collect(Collectors.toMap(PlatformPrice::getPlatformId, e -> e, (k1,  k2) -> k2));

        User one = userRepository.findOneByUserID(id);

        List<String> userIDs = Arrays.stream(one.getReferrer().split(",")).toList();

        List<PlatformPrice> platformPriceList = platformPriceRepository.findByUserIDs(userIDs);

        Map<String, Map<String, BigDecimal>> map = new HashMap<>();
        for (PlatformPrice platformPrice : platformPriceList) {
            if (map.containsKey(platformPrice.getUserID())) {
                map.get(platformPrice.getUserID()).put(platformPrice.getPlatformId(), platformPrice.getPrice());
            } else {
                map.put(platformPrice.getUserID(), new HashMap<>());
                map.get(platformPrice.getUserID()).put(platformPrice.getPlatformId(), platformPrice.getPrice());
            }
        }

        return accountPlatforms.stream().map(e -> {
            PlatformPriceRespDto platformPriceRespDto = new PlatformPriceRespDto();
            if (priceMap.containsKey(e.get_id())) {
                PlatformPrice platformPrice = priceMap.get(e.get_id());
                platformPriceRespDto.setPrice(platformPrice.getPrice());
            }
            platformPriceRespDto.setDefaultPrice(e.getPrice());
            for (int i = userIDs.size() - 1; i >= 0; i--) {
                if (map.containsKey(userIDs.get(i))) {
                    if (map.get(userIDs.get(i)).containsKey(e.get_id())) {
                        platformPriceRespDto.setDefaultPrice(map.get(userIDs.get(i)).get(e.get_id()));
                        break;
                    }
                }
            }

            platformPriceRespDto.setPlatformId(e.get_id());
            platformPriceRespDto.setPlatformName(e.getName());
            platformPriceRespDto.setUserID(id);
            return platformPriceRespDto;
        }).toList();

    }

    public void updatePrice(List<PlatformPriceRespDto> data, String userID) {
        for (PlatformPriceRespDto platformPriceRespDto : data) {
            if (platformPriceRespDto.getPrice() == null) {
                continue;
            }
            if (!userID.equals(Constants.ADMIN_USER_ID)) {
                User one = userRepository.findOneByUserID(platformPriceRespDto.getUserID());
                if (one == null || !one.getCreateUserID().equals(userID)) {
                    continue;
                }
            } else {
                User one = userRepository.findOneByUserID(platformPriceRespDto.getUserID());
                if (one == null) {
                    continue;
                }
                userID = one.getCreateUserID();
            }
            List<PlatformPriceRespDto> platformPriceRespDtoList = queryPrice(userID);
            Map<String, BigDecimal> map = platformPriceRespDtoList.stream().collect(Collectors.toMap(PlatformPriceRespDto::getPlatformId, e -> e.getPrice() == null ? e.getDefaultPrice() : e.getPrice(), (k1, k2) -> k2));

            // 代理价格不能低于用户的价格
            BigDecimal price2 = map.get(platformPriceRespDto.getPlatformId());
            if (!userID.equals(Constants.ADMIN_USER_ID)) {
                if (price2.compareTo(platformPriceRespDto.getPrice()) > 0) {
                    throw new CommonException("代理价格不能低于默认价格");
                }
            }

            PlatformPrice price = platformPriceRepository.findOneByUserIDAndPlatformId(platformPriceRespDto.getUserID(), platformPriceRespDto.getPlatformId());
            if (price == null) {
                price = new PlatformPrice();
                price.setPlatformId(platformPriceRespDto.getPlatformId());
                price.setUserID(platformPriceRespDto.getUserID());
                price.setPrice(platformPriceRespDto.getPrice());
                platformPriceRepository.save(price);
            } else {
                price.setPrice(platformPriceRespDto.getPrice());
                platformPriceRepository.update(price);
            }

            // 所有下级小于这个值的都更新
            processPriceUpdate(List.of(platformPriceRespDto.getUserID()), platformPriceRespDto.getPlatformId(), platformPriceRespDto.getPrice());
        }
    }

    public void processPriceUpdate(List<String> userIDs, String platformID, BigDecimal price) {
        if (userIDs.isEmpty()) {
            return;
        }
        List<User> ownUsers = userRepository.findUserByCreateUserIDs(userIDs);
        List<String> userIDs1 = new ArrayList<>();
        for (User user : ownUsers) {
            userIDs1.add(user.getUserID());
            PlatformPrice price2 = platformPriceRepository.findOneByUserIDAndPlatformId(user.getUserID(), platformID);
            if (price2 != null && price2.getPrice().compareTo(price) < 0) {
                price2.setPrice(price);
                platformPriceRepository.update(price2);
            }
        }
        if (!userIDs1.isEmpty()) {
            processPriceUpdate(userIDs1, platformID, price);
        }
    }

    public void openRecharge(List<String> ids) {
        userRepository.updateOpenRecharge(ids, "open");
    }

    public void closeRecharge(List<String> ids) {
        userRepository.updateOpenRecharge(ids, "close");
    }
}
