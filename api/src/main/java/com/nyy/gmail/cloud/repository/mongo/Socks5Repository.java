package com.nyy.gmail.cloud.repository.mongo;

import com.alibaba.fastjson2.JSON;
import com.mongodb.bulk.BulkWriteResult;
import com.nyy.gmail.cloud.common.enums.Socks5StatusEnum;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.MongoPageHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.IpCheckStatusEnum;
import com.nyy.gmail.cloud.enums.IpStatusEnum;
import com.nyy.gmail.cloud.model.dto.IdsListDTO;
import com.nyy.gmail.cloud.model.dto.Socks5ListDTO;
import com.nyy.gmail.cloud.model.result.AddIpResult;
import com.nyy.gmail.cloud.utils.IPPortValidator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Repository
public class Socks5Repository {

    private final ConcurrentMap<String, Socks5> cache = new ConcurrentHashMap<>();
    private final Map<String, TreeSet<Socks5>> areaIndex = new ConcurrentHashMap<>();
    private final Comparator<Socks5> socks5Comparator = Comparator
            .comparingLong(Socks5::getVpsCount)
            .thenComparing(Socks5::getLastUseTime)
            .thenComparing(Socks5::get_id); // 防止TreeSet重复元素

    // 初始化缓存
    @PostConstruct
    public void loadCache() {
        cache.clear();
        areaIndex.clear();
        Query query = new Query(Criteria.where("status").is("OK").and("proxyAccount").isNull()).limit(10000);
        List<Socks5> socks5List = mongoTemplate.find(query, Socks5.class);
        for (Socks5 s : socks5List) {
            if (s.getLastUseTime() == null) {
                s.setLastUseTime(new Date());
            }
            cache.put(s.get_id(), s);
            if (StringUtils.isNotBlank(s.getAreaCode())) {
                areaIndex.computeIfAbsent(s.getAreaCode(), k -> new TreeSet<>(socks5Comparator))
                        .add(s);
            } else {
                areaIndex.computeIfAbsent("", k -> new TreeSet<>(socks5Comparator))
                        .add(s);
            }
        }
    }

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoPageHelper mongoPageHelper;

    @Resource
    private AccountRepository accountRepository;


    public void saveSocks5(Socks5 socks5) {
        socks5 = mongoTemplate.insert(socks5);
    }

    public int saveSocks5List(List<Socks5> socks5List) {
        BulkOperations operations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Socks5.class);
        operations.insert(socks5List);
        try {
            BulkWriteResult result = operations.execute();
            log.info("表:【socks5】, 待插入记录条数:【{}】, 成功条数:【{}】", socks5List.size(), result.getInsertedCount());
            return result.getInsertedCount();
        } catch (BulkOperationException e) {
            BulkWriteResult result = e.getResult();
            log.info("表:【socks5】, 待插入记录条数:【{}】, 成功条数:【{}】", socks5List.size(), result.getInsertedCount());
            return result.getInsertedCount();
        } catch (Exception e) {
            log.error("表:【socks5】, 数据个数:【{}】 失败", socks5List.size(), e);
        }
        return 0;
    }

    /**
     * 校验IP和端口合法性，并移除不合法的元素
     * @param socks5List 要校验的列表
     * @return 非法IP/端口数量
     */
    public int checkIpOrPort(List<Socks5> socks5List) {
        if (socks5List == null || socks5List.isEmpty()) {
            return 0;
        }

        int errorCount = 0;
        // 使用迭代器安全删除元素
        Iterator<Socks5> iterator = socks5List.iterator();
        while (iterator.hasNext()) {
            Socks5 socks5 = iterator.next();
            try {
                boolean isValid = IPPortValidator.isValidPort(socks5.getPort(), true);

                if (!isValid) {
                    errorCount++;
                    iterator.remove(); // 安全删除当前元素
                    log.warn("Invalid Socks5 config removed - IP: {}, Port: {}", socks5.getIp(), socks5.getPort());
                }
            } catch (Exception e) {
                errorCount++;
                iterator.remove();
                log.error("Check IP/Port error, config: {}", JSON.toJSONString(socks5), e);
            }
        }
        return errorCount;
    }
    public AddIpResult saveSocks5ListV2(List<Socks5> socks5List) {
        AddIpResult addIpResult = new AddIpResult();
        addIpResult.setTotal(socks5List.size());
        Integer errorCount = checkIpOrPort(socks5List);
        addIpResult.setError(errorCount);

        BulkOperations operations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Socks5.class);
        operations.insert(socks5List);
        try {
            BulkWriteResult result = operations.execute();
            log.info("表:【socks5】, 待插入记录条数:【{}】, 成功条数:【{}】", socks5List.size(), result.getInsertedCount());
            addIpResult.setSuccess(result.getInsertedCount());
        } catch (BulkOperationException e) {
            BulkWriteResult result = e.getResult();
            log.info("表:【socks5】, 待插入记录条数:【{}】, 成功条数:【{}】", socks5List.size(), result.getInsertedCount());
            addIpResult.setSuccess(result.getInsertedCount());

        } catch (Exception e) {
            log.error("表:【socks5】, 数据个数:【{}】 失败", socks5List.size(), e);
        }
        addIpResult.setRepeat(socks5List.size() - addIpResult.getSuccess());
        return addIpResult;
    }


    public void updateSocks5(Socks5 socks5) {
        mongoTemplate.save(socks5);
    }

    public Socks5 findSocks5ById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Socks5 socks5 = mongoTemplate.findOne(query, Socks5.class);
        return socks5;
    }

    public Socks5 findSocks5ByIdUserID(String id, String userID) {
        Criteria criteria = Criteria.where("_id").is(id);
        if (!"admin".equals(userID)) {
            criteria.and("userID").is(userID);
        }
        Query query = new Query(criteria);
        Socks5 socks5 = mongoTemplate.findOne(query, Socks5.class);
        return socks5;
    }

    public void deleteSocks5ByIds(List<String> ids, String userID) {
        Query query = new Query(Criteria.where("userID").is(userID).and("_id").in(ids));
        mongoTemplate.remove(query, Socks5.class);

        // ip删除，批量下线在线的账号
        List<Account> onlineAccountList = accountRepository.findBySocks5IdListAndStatus(ids, AccountOnlineStatus.ONLINE.getCode());
        if (!CollectionUtils.isEmpty(onlineAccountList)) {
            for (Account account : onlineAccountList) {
            }
        }
    }

    public void updateSocksByBelongUser(List<String> ids, String userID, String belongUserID) {
        Query query = new Query(Criteria.where("userID").is(userID).and("_id").in(ids));
        Update update = new Update();
        update.set("belongUser", belongUserID);
        mongoTemplate.updateMulti(query, update, Socks5.class);
    }

    public void updateSocksByPlatform(List<String> ids, String userID, String platform) {
        Query query = new Query(Criteria.where("userID").is(userID).and("_id").in(ids));
        Update update = new Update();
        update.set("platform", platform);
        mongoTemplate.updateMulti(query, update, Socks5.class);
    }

    public Socks5 findAndUse(List<String> areaList, String account, int useLimit, String userID) {
//        Criteria criteria = Criteria.where("vpsCount").lt(useLimit)
//                .and("statusFlag").is(Socks5StatusEnum.OK.getValue());
//        if (!CollectionUtils.isEmpty(areaList)) {
//            criteria.and("areaCode").in(areaList);
//        }
//        if (StringUtils.isNotEmpty(userID)) {
//            criteria.and("userID").is(userID);
//        }
//        Query query = new Query(criteria)
//                .with(Sort.by(
//                        Sort.Order.asc("vpsCount"),
//                        Sort.Order.asc("lastUseTime")
//                )).limit(1);
//
//        Update update = new Update();
//        update.inc("useCount", 1)
//                .inc("vpsCount", 1)
//                .set("lastUseTime", new Date());
//        update.push("belongVps", account);
//        Socks5 socks5 = mongoTemplate.findAndModify(query, update, Socks5.class);
//        return socks5;
        if (cache.isEmpty() || areaIndex.isEmpty()) {
             Criteria criteria = Criteria.where("vpsCount").lt(useLimit)
                    .and("statusFlag").is(Socks5StatusEnum.OK.getValue());
            if (!CollectionUtils.isEmpty(areaList)) {
                criteria.and("areaCode").in(areaList);
            }
            if (StringUtils.isNotEmpty(userID)) {
                criteria.and("userID").is(userID);
            }
            Query query = new Query(criteria)
                    .with(Sort.by(
                            Sort.Order.asc("vpsCount"),
                            Sort.Order.asc("lastUseTime")
                    )).limit(1);

            Update update = new Update();
            update.inc("useCount", 1)
                    .inc("vpsCount", 1)
                    .set("lastUseTime", new Date());
            Socks5 socks5 = mongoTemplate.findAndModify(query, update, Socks5.class);
            return socks5;
        }

        Date now = new Date();

        // 合并所有目标区域的TreeSet
        TreeSet<Socks5> candidates = new TreeSet<>(socks5Comparator);
        if (areaList == null || areaList.isEmpty()) {
            areaIndex.values().forEach(candidates::addAll);
        } else {
            for (String area : areaList) {
                TreeSet<Socks5> set = areaIndex.get(area);
                if (set != null) candidates.addAll(set);
            }
        }

        for (Socks5 s : candidates) {
            if (s.getVpsCount() >= useLimit) continue;
            if (userID != null && !userID.equals(s.getUserID())) continue;
            if (s.getStatusFlag() != Socks5StatusEnum.OK.getValue()) continue;

            synchronized (s) { // 单条记录原子更新
                // 从TreeSet中删除再更新，保持排序正确
                if (StringUtils.isEmpty(s.getAreaCode())) {
                    s.setAreaCode("");
                }
                TreeSet<Socks5> set = areaIndex.get(s.getAreaCode());
                if (set != null) set.remove(s);

                s.setUseCount(s.getUseCount() + 1);
                s.setVpsCount(s.getVpsCount() + 1);
                s.setLastUseTime(now);

                if (set != null) set.add(s);
                return s;
            }
        }
        return null;
    }

    public Socks5 findAndUse() {
        Query query = new Query(Criteria.where("statusFlag").is(Socks5StatusEnum.OK.getValue()))
                .with(Sort.by(
                        Sort.Order.asc("lastUseTime")
                ));

        Update update = new Update()
                // .inc("useCount", 1)
                .set("lastUseTime", new Date());
        Socks5 socks5 = mongoTemplate.findAndModify(query, update, Socks5.class);
        return socks5;
    }

//    public long findAndRelease(String accid) {
//        Update update = new Update();
//        update.inc("vpsCount", -1);
//        update.pull("belongVps", accid);
//        Query query = new Query(Criteria.where("belongVps").is(accid));
//        return mongoTemplate.updateMulti(query, update, Socks5.class).getModifiedCount();
//    }
    public long findAndRelease(String accid) {
//        long count = 0;
//
//        for (Socks5 s : cache.values()) {
//            // 判断该记录是否包含该账号
//            if (s.getBelongVps().contains(accid)) {
//                synchronized (s) { // 保证单条记录更新原子性
//                    // 更新TreeSet索引前先移除旧元素
//                    TreeSet<Socks5> set = areaIndex.get(s.getAreaCode());
//                    if (set != null) set.remove(s);
//
//                    // 执行释放操作
//                    s.setVpsCount(Math.max(0, s.getVpsCount() - 1));
//                    s.getBelongVps().remove(accid);
//
//                    // 更新索引
//                    if (set != null) set.add(s);
//
//                    count++;
//                }
//            }
//        }
//
//        return count;
        return 1;
    }

    public AddIpResult addIp(List<Socks5> socks5List, String userID) {
        for (Socks5 socks5 : socks5List) {
            socks5.setUserID(userID);
            socks5.setBelongUser(userID);
            socks5.setStatus(IpStatusEnum.INIT.getCode());
            socks5.setIpCheckStatus(IpCheckStatusEnum.CHECKING.getCode());
            socks5.setCreateTime(new Date());
            socks5.setLastCheckTime(0);
            socks5.setLastCheckNormalTime(0);
            socks5.setVersion(0L);
        }
        return saveSocks5ListV2(socks5List);
    }
    public AddIpResult addIpSingle(List<Socks5> socks5List, String userID) {
        Socks5 socks5 = socks5List.getFirst();
        if (!IPPortValidator.isValidPort(socks5.getPort(), Boolean.TRUE)) {
            throw new CommonException("端口格式错误");
        }
        return addIp(socks5List, userID);
    }

    public void deleteSocks5ByIds(List<String> removeIds) {
        Query query = new Query(Criteria.where("_id").in(removeIds));
        mongoTemplate.remove(query, Socks5.class);
    }
}
