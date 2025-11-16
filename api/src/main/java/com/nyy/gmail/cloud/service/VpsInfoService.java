package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.entity.mysql.VpsInfo;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.model.dto.vps.*;
import com.nyy.gmail.cloud.model.vo.vps.VpsInfoVO;
import com.nyy.gmail.cloud.model.vo.vps.VpsPageResult;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.BalanceDetailRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.repository.mysql.VpsInfoRepository;
import com.nyy.gmail.cloud.utils.DateUtil;
import com.nyy.gmail.cloud.utils.UUIDUtils;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VpsInfoService {

    @Resource
    private VpsInfoRepository vpsInfoRepository;

    @Resource
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ParamsService paramsService;

    @Resource
    private BalanceDetailRepository balanceDetailRepository;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 购买设备
     */
    public Result<BigDecimal> buyVps(VpsBuyDTO dto) {

        dto.setPrice(new BigDecimal(paramsService.getParams("vps.vpsUnitPrice", 1, null).toString()));
        if (!StringUtils.hasText(dto.getUserId()) || dto.getMonthCount() == null || dto.getAmount() == null || dto.getPrice() == null) {
            return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID);
        }
        User user = userRepository.findOneByUserID_(dto.getUserId());
        if (user == null) {
            return ResponseResult.failure(ResultCode.NO_ACCOUNT);
        }
        Integer countVps = vpsInfoRepository.countVpsInfoByDeadTimeIsNull();
        if (countVps < dto.getAmount()) {
            return ResponseResult.failure(ResultCode.REACH_USE_DEVICE);
        }
        //TODO 余额操作，分布式锁
        RLock lock = redissonClient.getLock(user.getUserID() + "-charge");
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    BigDecimal amount = dto.getPrice().multiply(new BigDecimal(dto.getMonthCount() * dto.getAmount()));
                    if (user.getBalance().compareTo(amount) < 0) {
                        return ResponseResult.failure(ResultCode.INSUFFICIENT_USER_BALANCE);
                    }
                    BigDecimal afterValue = user.getBalance().subtract(amount);
                    user.setBalance(afterValue);
                    userRepository.updateUser(user);
                    String billDesc = "购买" + dto.getAmount() + "台设备" + dto.getMonthCount() + "月";
                    balanceDetailRepository.addUserBill(billDesc, user.getUserID(),
                            amount, afterValue, user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.BUY_VPS);

                    //生成设备记录
                    List<VpsInfo> list = vpsInfoRepository.queryVpsInfoByDeadTimeIsNullOrderById(Limit.of(dto.getAmount()));
                    String uuId = UUIDUtils.get32UUId();
                    list.forEach(info -> {
                        info.setUserId(dto.getUserId());
                        info.setBindStatus(BindStatusEnums.UNBIND.getStatus());
                        info.setRunStatus(RunStatusEnums.RUNNING.getStatus());
                        info.setBatchId(uuId);
                        info.setDeadTime(DateUtil.getMonthsBeforeDate(-dto.getMonthCount()));
                    });
                    vpsInfoRepository.saveAll(list);
                    return ResponseResult.success(afterValue);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
        }
        return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
    }

    public Result<List<VpsInfoDTO>> queryDeadTimeByUserId(VpsQueryDTO dto) {

        List<VpsInfo> list;
        if (StringUtils.isEmpty(dto.getUserId())) {
            list = vpsInfoRepository.findAll();
        } else {
            list = vpsInfoRepository.queryVpsInfoByUserIdOrderByDeadTimeDesc(dto.getUserId());
        }

        if (CollectionUtils.isEmpty(list)) {
            return ResponseResult.failure(ResultCode.DATA_NOT_EXISTED);
        }
        List<VpsInfoDTO> dtos = new ArrayList<>();
        Map<String, List<VpsInfo>> map = list.stream().filter(v -> v.getBatchId() != null).collect(Collectors.groupingBy(VpsInfo::getBatchId));
        map.forEach((key, vpsInfos) -> {
            VpsInfoDTO vpsInfoDTO = new VpsInfoDTO();
            vpsInfoDTO.setDeadTime(vpsInfos.getFirst().getDeadTime());
            vpsInfoDTO.setBatchId(key);
            vpsInfoDTO.setUnBindCount(vpsInfos.stream().filter(v -> BindStatusEnums.UNBIND.getStatus().equals(v.getBindStatus())).toList().size());
            vpsInfoDTO.setBindCount(vpsInfos.stream().filter(v -> BindStatusEnums.BINDING.getStatus().equals(v.getBindStatus())).toList().size());
            vpsInfoDTO.setVpsCount(vpsInfos.size());
            dtos.add(vpsInfoDTO);
        });

        dtos.sort((p1, p2) -> p2.getDeadTime().compareTo(p1.getDeadTime()));
        List<VpsInfo> wList = list.stream().filter(v -> v.getDeadTime() == null).toList();
        if (!CollectionUtils.isEmpty(wList)) {
            VpsInfoDTO vpsInfoDTO = new VpsInfoDTO();
            vpsInfoDTO.setUnBindCount(wList.size());
            vpsInfoDTO.setBindCount(0);
            vpsInfoDTO.setVpsCount(wList.size());
            dtos.add(vpsInfoDTO);
        }
        return ResponseResult.success(dtos);
    }

    /**
     * 更具时间批次续期
     */
    public Result<BigDecimal> batchRenewByDateBatch(VpsRenewDTO dto) {

        // 过滤dto.batchIds 元素为null的
        dto.setBatchIds(dto.getBatchIds().stream().filter(Objects::nonNull).toList());
        if (CollectionUtils.isEmpty(dto.getBatchIds())) {
            return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID);
        }
        if (!StringUtils.hasText(dto.getUserId()) || CollectionUtils.isEmpty(dto.getBatchIds())) {
            return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID);
        }

        List<VpsInfo> list = vpsInfoRepository.queryVpsInfoByBatchIdIn(dto.getBatchIds());
        List<String> userIdList = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
            return ResponseResult.failure(ResultCode.DATA_NOT_EXISTED);
        }
        userIdList = list.stream().map(VpsInfo::getUserId).distinct().toList();

        User user = userRepository.findOneByUserID_(dto.getUserId());
        if (user == null) {
            return ResponseResult.failure(ResultCode.NO_ACCOUNT);
        }
        //TODO 余额操作，分布式锁
        RLock lock = redissonClient.getLock(user.getUserID() + "-charge");
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    BigDecimal amount = BigDecimal.ZERO;
                    if (dto.getDeadTime() == null) {
                        amount = new BigDecimal(paramsService.getParams("vps.vpsUnitPrice", 1, null).toString()).multiply(new BigDecimal(dto.getMonthCount() * list.size()));
                        if (user.getBalance().compareTo(amount) < 0) {
                            return ResponseResult.failure(ResultCode.INSUFFICIENT_USER_BALANCE);
                        }
                        BigDecimal afterValue = user.getBalance().subtract(amount);
                        user.setBalance(afterValue);
                        userRepository.updateUser(user);

                        String billDesc = list.size() + "台设备续费" + dto.getMonthCount() + "月";
                        balanceDetailRepository.addUserBill(billDesc, user.getUserID(),
                                amount, afterValue, user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.CHARGE_VPS);

                        // 延期
                    } else {
                        if (!Constants.ADMIN_USER_ID.equals(dto.getUserId())) {
                            return ResponseResult.failure(ResultCode.METHOD_NOT_ALLOWED);
                        }
                        String billDesc = "为 " + String.join(",", userIdList) + " " + list.size() + "台设备延期至" + DateUtil.formatByDate(dto.getDeadTime(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM);
                        balanceDetailRepository.addUserBill(billDesc,
                                Constants.ADMIN_USER_ID, BigDecimal.ZERO, user.getBalance(), Constants.ADMIN_USER_ID, BillExpenseTypeEnums.OUT, BillCateTypeEnums.DELAY_VPS);
                    }
                    //更新设备记录
                    list.forEach(e -> {
                        if (dto.getDeadTime() != null) {
                            e.setDeadTime(dto.getDeadTime());
                        } else {
                            e.setDeadTime(DateUtil.getMonthsDate(e.getDeadTime(), dto.getMonthCount()));
                        }

                    });
                    vpsInfoRepository.saveAll(list);
                    return ResponseResult.success(user.getBalance().subtract(amount));
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
        }
        return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
    }

    public VpsPageResult<VpsInfoVO> queryVpsInfoByPage(VpsQueryDTO dto) {


        dto.getFilters().put("userId", dto.getUserId());
        if (dto.getFilters().get("vpsId") != null && "".equals(dto.getFilters().get("vpsId"))) {
            dto.getFilters().remove("vpsId");
        }

        Specification<VpsInfo> spec = new Specification<VpsInfo>() {
            @Override
            public Predicate toPredicate(Root<VpsInfo> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

                List<Predicate> predicates = new ArrayList<>();

                if (StringUtils.hasText(dto.getUserId())) {
                    Predicate p1 = criteriaBuilder.equal(root.get("userId"), dto.getUserId());
                    predicates.add(p1);
                }
                if (dto.getFilters().get("runStatus") != null) {
                    Predicate p2 = criteriaBuilder.equal(root.get("runStatus"), dto.getFilters().get("runStatus"));
                    predicates.add(p2);
                }
                if (dto.getFilters().get("vpsId") != null) {
                    Predicate p5 = criteriaBuilder.equal(root.get("vpsId"), dto.getFilters().get("vpsId"));
                    predicates.add(p5);
                }
                if (dto.getFilters().get("userBind") != null) {
                    Predicate p6;
                    if (BindStatusEnums.BINDING.getStatus().equals(dto.getFilters().get("userBind"))) {
                        p6 = criteriaBuilder.isNotNull(root.get("userId"));
                    } else {
                        p6 = criteriaBuilder.isNull(root.get("userId"));
                    }
                    predicates.add(p6);
                }
                if (dto.getFilters().get("bindStatus") != null) {

                    Predicate p3 = criteriaBuilder.equal(root.get("bindStatus"), dto.getFilters().get("bindStatus"));
                    predicates.add(p3);
                }
                if (dto.getFilters().get("deadStatus") != null) {
                    String deadStatus = (String) dto.getFilters().get("deadStatus");
                    if (DeadStatusEnums.EXPIRED.getStatus().equals(deadStatus)) {
                        Predicate p4 = criteriaBuilder.lessThan(root.get("deadTime"), new Date());
                        predicates.add(p4);
                    } else if (DeadStatusEnums.TO_EXPIRE.getStatus().equals(deadStatus)) {
                        Date data = DateUtil.getDateAfterDate(new Date(), 5);
                        Predicate p4 = criteriaBuilder.between(root.get("deadTime"), new Date(), data);
                        predicates.add(p4);
                    } else {
                        Date data = DateUtil.getDateAfterDate(new Date(), 5);
                        Predicate p4 = criteriaBuilder.greaterThan(root.get("deadTime"), data);
                        predicates.add(p4);
                    }
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };

        Pageable pageable = PageRequest.of(dto.getPageNo() - 1, dto.getPageSize());
        Page<VpsInfo> pageResult = vpsInfoRepository.findAll(spec, pageable);

        List<VpsInfoVO> vos = new ArrayList<>();
        pageResult.getContent().forEach(e -> {
            VpsInfoVO info = new VpsInfoVO();
            BeanUtils.copyProperties(e, info);
            Account ac = accountRepository.findById(e.getAccid());
            if (ac != null) {
                info.setAccName(ac.getPhone());
            }
            vos.add(info);
        });
        VpsPageResult<VpsInfoVO> prv = new VpsPageResult<>();
        prv.setData(vos);
        prv.setTotal(pageResult.getTotalElements());
        prv.setPages(pageResult.getTotalPages());
        prv.setPageSize(dto.getPageSize());
        prv.setPageNum(dto.getPageNo());

        List<VpsInfo> list = vpsInfoRepository.findAll(spec);
        Map<String, Integer> runStatusCount = new HashMap<>();
        Integer runCount = list.stream().filter(e -> RunStatusEnums.RUNNING.getStatus().equals(e.getRunStatus())).toList().size();
        runStatusCount.put(RunStatusEnums.RUNNING.getStatus(), runCount);
        runStatusCount.put(RunStatusEnums.STOP.getStatus(), (int) (pageResult.getTotalElements() - runCount));

        Map<String, Integer> deadStatusCount = new HashMap<>();
        Date data = DateUtil.getDateAfterDate(new Date(), 5);
        Integer deadCount = list.stream().filter(e -> e.getDeadTime() != null).filter(e -> e.getDeadTime().before(new Date())).toList().size();
        Integer undeadCount = list.stream().filter(e -> e.getDeadTime() != null).filter(e -> e.getDeadTime().after(data)).toList().size()
                + list.stream().filter(e -> e.getDeadTime() == null).toList().size();
        deadStatusCount.put(DeadStatusEnums.EXPIRED.getStatus(), deadCount);
        deadStatusCount.put(DeadStatusEnums.TO_EXPIRE.getStatus(), (int) (pageResult.getTotalElements() - deadCount - undeadCount));
        deadStatusCount.put(DeadStatusEnums.VALLID.getStatus(), undeadCount);
        if (dto.getFilters().get("deadStatus") != null) {
            String deadStatus = (String) dto.getFilters().get("deadStatus");
            deadStatusCount.put(DeadStatusEnums.EXPIRED.getStatus(), 0);
            deadStatusCount.put(DeadStatusEnums.TO_EXPIRE.getStatus(), 0);
            deadStatusCount.put(DeadStatusEnums.VALLID.getStatus(), 0);
            deadStatusCount.put(deadStatus, (int) pageResult.getTotalElements());
        }

        Map<String, Integer> bindStatusCount = new HashMap<>();
        Integer bindCount = list.stream().filter(e -> BindStatusEnums.BINDING.getStatus().equals(e.getBindStatus())).toList().size();
        bindStatusCount.put(BindStatusEnums.BINDING.getStatus(), bindCount);
        bindStatusCount.put(BindStatusEnums.UNBIND.getStatus(), (int) (pageResult.getTotalElements() - bindCount));

        Map<String, Integer> userBindCount = new HashMap<>();
        Integer bCount = list.stream().filter(e -> e.getUserId() != null).toList().size();
        userBindCount.put(BindStatusEnums.BINDING.getStatus(), bCount);
        userBindCount.put(BindStatusEnums.UNBIND.getStatus(), (int) (pageResult.getTotalElements() - bCount));

        prv.setBindStatusCount(bindStatusCount);
        prv.setRunStatusCount(runStatusCount);
        prv.setDeadStatusCount(deadStatusCount);
        prv.setUserBindCount(userBindCount);
        return prv;
    }

    public Result<Boolean> updateDesc(String vpsId, String desc, String userID) {

        VpsInfo info = vpsInfoRepository.queryVpsInfoByVpsId(vpsId);
        if (info == null || !info.getUserId().equals(userID)) {
            return ResponseResult.failure(ResultCode.DATA_NOT_EXISTED);
        }
        info.setDescription(desc);
        vpsInfoRepository.save(info);
        return ResponseResult.success(true);
    }

    public Result addVps(VpsAddDTO dto) {

        if (dto.getAddNum() == null) {
            return ResponseResult.failure(ResultCode.PARAMS_IS_INVALID);
        }
        //生成设备记录
        List<VpsInfo> list = new ArrayList<>();
        for (int i = 0; i < dto.getAddNum(); i++) {
            VpsInfo info = new VpsInfo();
            info.setBindStatus(BindStatusEnums.UNBIND.getStatus());
            info.setRunStatus(RunStatusEnums.RUNNING.getStatus());
            info.setVpsId(UUIDUtils.get32UUId());
            info.setUseLock(false);
            list.add(info);
        }
        vpsInfoRepository.saveAll(list);
        return ResponseResult.success(true);
    }
}
