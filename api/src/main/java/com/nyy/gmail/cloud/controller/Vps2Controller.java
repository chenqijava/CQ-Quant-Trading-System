package com.nyy.gmail.cloud.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.BalanceDetail;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.entity.mongo.Vps;
import com.nyy.gmail.cloud.enums.BillCateTypeEnums;
import com.nyy.gmail.cloud.enums.BillExpenseTypeEnums;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.repository.mongo.BalanceDetailRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.service.ParamsService;
import com.nyy.gmail.cloud.service.VpsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping({"/api/vps2"})
public class Vps2Controller {

    @Resource
    private VpsService vpsService;

    @Autowired
    private MongoPaginationHelper mongoPaginationHelper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParamsService paramsService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private BalanceDetailRepository balanceDetailRepository;


    @PostMapping("{pageSize}/{page}")
    public Result<VpsRespDTO> list(@PathVariable("pageSize") int pageSize, @PathVariable("page") int page, @RequestBody(required = false) VpsReqDTO vpsReqDTO) {
        if (vpsReqDTO.getFilters() == null) {
            vpsReqDTO.setFilters(new HashMap<>());
        }
        String userID = Session.currentSession().userID;

        User user = userRepository.findOneByUserID(userID);
        String vpsUnitPrice = paramsService.getParams("vps.vpsUnitPrice", 1, null).toString();
        BigDecimal balance = user.getBalance() == null ? BigDecimal.ZERO :user.getBalance();

        if (!userID.equals("admin")) {
            vpsReqDTO.getFilters().put("userID", userID);
        }
        for (String key : vpsReqDTO.getFilters().keySet()) {
            Object object = vpsReqDTO.getFilters().get(key);
            if (object instanceof Map && ((Map<String, Object>)object).containsKey("$in")) {

            } else if (key.equals("userBind")) {
                if (vpsReqDTO.getFilters().get(key).equals("1")) {
                    HashMap<Object, Object> map = new HashMap<>();
                    map.put("$ne", "");
                    vpsReqDTO.getFilters().put("userID", map);
                } else {
                    vpsReqDTO.getFilters().put("userID", "");
                }
            } else if (key.equals("userID") || key.equals("runStatus") || key.equals("deadStatus") || key.equals("bindStatus") || key.equals("bindType") || key.equals("onlineStatus") || key.equals("loginStatus")) {

            } else {
                HashMap<Object, Object> map = new HashMap<>();
                map.put("$regex", vpsReqDTO.getFilters().get(key));
                vpsReqDTO.getFilters().put(key, map);
            }
        }

        PageResult<Vps> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(Vps.class)
                .filters(vpsReqDTO.getFilters())
                .sorter(vpsReqDTO.getSorter())
                .pageSize(pageSize)
                .page(page)
                .build());

        PageResult<Vps> total = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(Vps.class)
                .filters(vpsReqDTO.getFilters())
                .sorter(vpsReqDTO.getSorter())
                .pageSize(10000000)
                .page(1)
                .build());
        Map<String, Integer> userBindCount = new HashMap<>();
        userBindCount.put("0", 0);
        userBindCount.put("1", 0);
        if (userID.equals("admin")) {
            userBindCount.put("0", (int)total.getData().stream().filter(e -> StringUtils.isEmpty(e.getUserID())).count());
            userBindCount.put("1", (int)total.getData().stream().filter(e -> StringUtils.isNotEmpty(e.getUserID())).count());
        }

        Map<String, Integer> runStatusCount = new HashMap<>();
        runStatusCount.put("0",  (int)total.getData().stream().filter(e -> e.getRunStatus().equals("0")).count());
        runStatusCount.put("1",  (int)total.getData().stream().filter(e -> e.getRunStatus().equals("1")).count());

        Map<String, Integer> deadStatusCount = new HashMap<>();
        deadStatusCount.put("0",  (int)total.getData().stream().filter(e -> e.getDeadStatus().equals("0")).count());
        deadStatusCount.put("1",  (int)total.getData().stream().filter(e -> e.getDeadStatus().equals("1")).count());
        deadStatusCount.put("2",  (int)total.getData().stream().filter(e -> e.getDeadStatus().equals("2")).count());

        Map<String, Integer> bindStatusCount = new HashMap<>();
        bindStatusCount.put("0", (int)total.getData().stream().filter(e -> e.getBindStatus().equals("0")).count());
        bindStatusCount.put("1", (int)total.getData().stream().filter(e -> e.getBindStatus().equals("1")).count());

        VpsRespDTO vpsRespDTO = new VpsRespDTO();
        vpsRespDTO.setBalance(balance);
        vpsRespDTO.setData(pageResult.getData());
        vpsRespDTO.setTotal(pageResult.getTotal());
        vpsRespDTO.setUnitPrice(vpsUnitPrice);
        vpsRespDTO.setRunStatusCount(runStatusCount);
        vpsRespDTO.setDeadStatusCount(deadStatusCount);
        vpsRespDTO.setBindStatusCount(bindStatusCount);
        vpsRespDTO.setUserBindCount(userBindCount);

        return ResponseResult.success(vpsRespDTO);
    }


    @PostMapping("deadTimeList/{pageSize}/{page}")
    public Result<List<DeadTimeListRespDTO>> deadTimeList (@PathVariable("pageSize") int pageSize, @PathVariable("page") int page, @RequestBody(required = false) DeadTimeListReqDTO deadTimeListReqDTO) {
        if (deadTimeListReqDTO.getFilters() == null) {
            deadTimeListReqDTO.setFilters(new HashMap<>());
        }
        String userID = Session.currentSession().userID;

        if (!userID.equals("admin")) {
            deadTimeListReqDTO.getFilters().put("userID", userID);
        }
        for (String key : deadTimeListReqDTO.getFilters().keySet()) {
            if (key.equals("userID")) {

            } else {
                HashMap<Object, Object> map = new HashMap<>();
                map.put("$regex", deadTimeListReqDTO.getFilters().get(key));
                deadTimeListReqDTO.getFilters().put(key, map);
            }
        }

        PageResult<Vps> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(Vps.class)
                .filters(deadTimeListReqDTO.getFilters())
                .sorter(deadTimeListReqDTO.getSorter())
                .pageSize(pageSize)
                .page(page)
                .build());
        Date now = new Date();
        long threeDayTime = now.getTime() + 3 * 24 * 60 * 60 * 1000;
        Map<String, DeadTimeListRespDTO> deadTimeMap = new HashMap<>();
        List<DeadTimeListRespDTO> deadTimeList = new ArrayList<>();
        for (Vps vps : pageResult.getData()) {
            Long dt = vps.getDeadTime() != null ? vps.getDeadTime().getTime() : null;
            String key = vps.getUserID() + "-" + (dt != null ? dt : "null");
            if (!deadTimeMap.containsKey(key)) {
                DeadTimeListRespDTO deadTimeListRespDTO = new DeadTimeListRespDTO();
                deadTimeListRespDTO.setDeadStatus("0");
                deadTimeListRespDTO.setDeadTime(vps.getDeadTime());
                deadTimeListRespDTO.set_id(key);
                deadTimeListRespDTO.setBind(0L);
                deadTimeListRespDTO.setUnbind(0L);
                deadTimeListRespDTO.setTotal(0L);
                deadTimeListRespDTO.setUserID(vps.getUserID());
                if (dt != null) {
                    if (dt.compareTo(now.getTime()) < 0) {
                        deadTimeListRespDTO.setDeadStatus("2");
                    } else if (dt.compareTo(threeDayTime) <= 0 && dt.compareTo(now.getTime()) >= 0) {
                        deadTimeListRespDTO.setDeadStatus("1");
                    }
                }
                deadTimeMap.put(key, deadTimeListRespDTO);
                deadTimeList.add(deadTimeListRespDTO);
            }
            if (vps.getBindStatus().equals("1")) {
                deadTimeMap.get(key).setBind(deadTimeMap.get(key).getBind() + 1);
            } else {
                deadTimeMap.get(key).setUnbind(deadTimeMap.get(key).getUnbind() + 1);
            }
            deadTimeMap.get(key).setTotal(deadTimeMap.get(key).getTotal() + 1);
        }

        return ResponseResult.success(deadTimeList);

    }

    @GetMapping("bindVpsCount")
    public Result<BindVpsCountDTO> bindVpsCount () {
        String userID = Session.currentSession().userID;

        Query query = new Query(Criteria.where("userID").is(userID));
        List<Vps> vps = mongoTemplate.find(query, Vps.class);

        int total = vps.size();

        long binds = vps.stream().filter(e -> e.getBindStatus().equals("1")).count();

        long unbinds = vps.stream().filter(e -> e.getBindStatus().equals("0") && !"01".contains(e.getDeadStatus())).count();

        BindVpsCountDTO bindVpsCountDTO = new BindVpsCountDTO();
        bindVpsCountDTO.setTotal(total);
        bindVpsCountDTO.setBinds(binds);
        bindVpsCountDTO.setUnbinds(unbinds);
        return ResponseResult.success(bindVpsCountDTO);
    }

    @PostMapping("delete")
    public Result delete (@RequestBody(required = false) IdsListDTO ids) {
        String userID = Session.currentSession().userID;
        if (userID.equals("admin")) {
            Query query = new Query(Criteria.where("id").in(ids.getIds()));
            mongoTemplate.remove(query, Vps.class);
        }

        return ResponseResult.success();
    }



    @PostMapping("machineScopeVpsCount")
    public Result<Long> machineScopeVpsCount (@RequestBody(required = false) MachineScopeVpsCountReqDTO data) {
        String userID = Session.currentSession().userID;
        List<String> vpsId = new ArrayList<>();
        if (data.getEndMachine() != null && data.getStartMachine() != null) {
            for (int i = data.getStartMachine(); i <= data.getEndMachine(); i++) {
                vpsId.add(i+"");
            }
        }
        Query query = new Query(Criteria.where("userID").is(userID).and("vpsID").in(vpsId));
        long count = mongoTemplate.count(query, Vps.class);

        return ResponseResult.success(count);
    }

    @PostMapping("addVps")
    public Result addVps (@RequestBody(required = false) Map<String, Integer> data) {
        String userID = Session.currentSession().userID;
        if (!userID.equals("admin")) {
            return ResponseResult.failure(ResultCode.BAD_REQUEST);
        }
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "createTime"));
        Vps vps = mongoTemplate.findOne(query, Vps.class);
        int startNum = 0;
        if (vps != null) {
            startNum = Integer.valueOf(vps.getVpsID());
        }
        for (int i = 1; i <= data.get("addNum"); i++) {
            Query query2 = new Query((Criteria.where("vpsID").is(startNum + i + "")));
            vps = mongoTemplate.findOne(query2, Vps.class);
            if (vps == null) {
                Vps vps1 = new Vps();
                vps1.setVpsID(startNum + i + "");
                vps1.setRunStatus("1");
                vps1.setUserID("");
                vps1.setUseLock(false);
                vps1.setCreateTime(new Date());
                vps1.setDeadStatus("0");
                vps1.setBindStatus("0");
                mongoTemplate.insert(vps1);
            }
        }

        return ResponseResult.success();
    }


    @PostMapping("delay")
    public Result adminDelayVps (@RequestBody(required = true) AdminDelayVpsReqDTO data) {
        String userID = Session.currentSession().userID;
        if (userID.equals("admin")) {
            List<Vps> vpsList = new ArrayList<>();
            if (data.getIds() != null) {
                Query query = new Query(Criteria.where("_id").in(data.getIds()));
                vpsList = mongoTemplate.find(query, Vps.class);
            } else if (data.getDeadTimes() != null) {
                for (String dt : data.getDeadTimes()) {
                    String[] ud = dt.split("-");
                    if (!ud[0].isEmpty()) {
                        Date date = new Date(Long.valueOf(ud[1]));
                        Date nextMs = new Date(date.getTime() + 1000);
                        Query query = new Query(Criteria.where("deadTime").lt(nextMs).gte(date).and("userID").is(ud[0]));
                        vpsList.addAll(mongoTemplate.find(query, Vps.class));
                    }
                }
            } else if (data.getEndMachine() != null && data.getStartMachine() != null) {
                List<String> vpsId = new ArrayList<>();
                if (data.getEndMachine() != null && data.getStartMachine() != null) {
                    for (int i = data.getStartMachine(); i <= data.getEndMachine(); i++) {
                        vpsId.add(i+"");
                    }
                }
                Query query = new Query(Criteria.where("vpsID").in(vpsId));
                vpsList = mongoTemplate.find(query, Vps.class);
            }


            if (!vpsList.isEmpty()) {
                User user = userRepository.findOneByUserID(userID);
                RLock lock = redissonClient.getLock("updateVpsDeadStatus");
                try {
                    if (lock.tryLock(30, TimeUnit.SECONDS)) {
                        try {
                            String desc = vpsList.size() + "台设备延期至" + data.getChargeDelayValue() + " 23:59:59";
                            balanceDetailRepository.addUserBill(desc,
                                    Constants.ADMIN_USER_ID,BigDecimal.ZERO, user.getBalance(),
                                    Constants.ADMIN_USER_ID, BillExpenseTypeEnums.OUT, BillCateTypeEnums.DELAY_VPS
                                    );
                            for (Vps v : vpsList) {
                                DateTime dateTime = DateUtil.parse(data.getChargeDelayValue(), "yyyy-MM-dd");
                                dateTime.setHours(23);
                                dateTime.setMinutes(59);
                                dateTime.setSeconds(59);
                                v.setDeadTime(dateTime.toJdkDate());
                                v.setDeadStatus("0");
                                v.setRunStatus("1");
                                mongoTemplate.save(v);
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        return ResponseResult.success();
    }




    @PostMapping("buy")
    public Result userBuyVps (@RequestBody(required = true) UserBuyVpsReqDTO data) {
        String userID = Session.currentSession().userID;
        Query query =  new Query(Criteria.where("useLock").is(false).and("runStatus").is("1").and("userID").is(""));
        List<Vps> vpsList = mongoTemplate.find(query, Vps.class);
        Map<String, Object> filters = new HashMap<>();
        filters.put("useLock", false);
        filters.put("runStatus", "1");
        filters.put("userID", "");
        PageResult<Vps> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(Vps.class)
                .filters(filters)
                .pageSize(data.getBuyMachineValue())
                .page(1)
                .build());
        vpsList = pageResult.getData();
        String vpsUnitPrice = paramsService.getParams("vps.vpsUnitPrice", 1, null).toString();
        BigDecimal total = BigDecimal.valueOf(pageResult.getData().size()).multiply(BigDecimal.valueOf(data.getBuyMonthValue())).multiply(new BigDecimal(vpsUnitPrice)).setScale(2);
        User user = userRepository.findOneByUserID(userID);
        if (total.compareTo(user.getBalance()) > 0) {
            return ResponseResult.failure(ResultCode.INSUFFICIENT_USER_BALANCE);
        }

        long time = new Date().getTime() + (long) data.getBuyMonthValue() * 30 * 24 * 60 * 60 * 1000;
        Date date = new Date(time);
        RLock lock = redissonClient.getLock(user.get_id());
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    RLock lock2 = redissonClient.getLock("updateVpsDeadStatus");
                    if (lock2.tryLock()) {
                        try {
                            user = userRepository.findOneByUserID(userID);
                            total = BigDecimal.valueOf(vpsList.size()).multiply(BigDecimal.valueOf(data.getBuyMonthValue())).multiply(new BigDecimal(vpsUnitPrice)).setScale(2);
                            user.setBalance(user.getBalance().subtract(total));
                            mongoTemplate.save(user);
                            BalanceDetail balanceDetail = new BalanceDetail();
                            balanceDetail.setName(user.getName());
                            balanceDetail.setUserID(userID);
                            balanceDetail.setExpenseType("2");
                            balanceDetail.setType("buyVps");
                            balanceDetail.setValue(total);
                            balanceDetail.setBalance(user.getBalance());
                            balanceDetail.setDescription("购买"+vpsList.size()+"台设备"+data.getBuyMonthValue()+"月");
                            balanceDetail.setCreateTime(new Date());
                            mongoTemplate.insert(balanceDetail);
                            for (Vps vps : vpsList) {
                                vps.setUserID(userID);
                                vps.setDeadStatus("0");
                                vps.setDeadTime(date);
                                vps.setBindStatus("0");
                                mongoTemplate.save(vps);
                            }

                        } finally {
                            lock2.unlock();
                        }
                    }

                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
        }

        return ResponseResult.success();
    }

    @PostMapping("userChargeVps")
    public Result userChargeVps (@RequestBody(required = true) UserChargeVpsReqDTO data) {
        String userID = Session.currentSession().userID;
        List<Vps> vpsList = new ArrayList<>();
        if (data.getIds() != null) {
            Query query = new Query(Criteria.where("_id").in(data.getIds()).and("userID").is(userID));
            vpsList = mongoTemplate.find(query, Vps.class);
        } else if (data.getDeadTimes() != null) {
            for (String dt : data.getDeadTimes()) {
                String[] ud = dt.split("-");
                if (!ud[0].isEmpty()) {
                    Date date = new Date(Long.valueOf(ud[1]));
                    Date nextMs = new Date(date.getTime() + 1000);
                    Query query = new Query(Criteria.where("deadTime").lt(nextMs).gte(date).and("userID").is(userID));
                    vpsList.addAll(mongoTemplate.find(query, Vps.class));
                }
            }
        } else if (data.getEndMachine() != null && data.getStartMachine() != null) {
            List<String> vpsId = new ArrayList<>();
            if (data.getEndMachine() != null && data.getStartMachine() != null) {
                for (int i = data.getStartMachine(); i <= data.getEndMachine(); i++) {
                    vpsId.add(i+"");
                }
            }
            Query query = new Query(Criteria.where("vpsID").in(vpsId).and("userID").is(userID));
            vpsList = mongoTemplate.find(query, Vps.class);
        }

        if (vpsList.size() > 0) {
            String vpsUnitPrice = paramsService.getParams("vps.vpsUnitPrice", 1, null).toString();
            User user = userRepository.findOneByUserID(userID);
            BigDecimal cost = BigDecimal.valueOf(vpsList.size()).multiply(BigDecimal.valueOf(data.getChargeMonthValue())).multiply(new BigDecimal(vpsUnitPrice)).setScale(2);
            if (cost.compareTo(user.getBalance()) > 0) {
                return ResponseResult.failure(ResultCode.INSUFFICIENT_USER_BALANCE);
            }

            RLock lock = redissonClient.getLock(user.get_id());
            try {
                if (lock.tryLock(30, TimeUnit.SECONDS)) {
                    try {
                        RLock lock2 = redissonClient.getLock(user.get_id());
                        if (lock2.tryLock()) {
                            try {
                                user = userRepository.findOneByUserID(userID);
                                cost = BigDecimal.valueOf(vpsList.size()).multiply(BigDecimal.valueOf(data.getChargeMonthValue())).multiply(new BigDecimal(vpsUnitPrice)).setScale(2);
                                user.setBalance(user.getBalance().subtract(cost));
                                mongoTemplate.save(user);
                                BalanceDetail balanceDetail = new BalanceDetail();
                                balanceDetail.setName(user.getName());
                                balanceDetail.setUserID(userID);
                                balanceDetail.setExpenseType("2");
                                balanceDetail.setType("chargeVps");
                                balanceDetail.setValue(cost);
                                balanceDetail.setBalance(user.getBalance());
                                balanceDetail.setDescription(vpsList.size()+"台设备续费"+data.getChargeMonthValue()+"月");
                                balanceDetail.setCreateTime(new Date());
                                mongoTemplate.insert(balanceDetail);
                                Date now = new Date();
                                long ms = (long) data.getChargeMonthValue() * 30 * 24 * 60 * 60 * 1000;
                                Date date = new Date(now.getTime() + ms);
                                for (Vps vps : vpsList) {
                                    if (vps.getDeadTime().getTime() <= now.getTime()) {
                                        vps.setDeadTime(date);
                                    } else {
                                        vps.setDeadTime(new Date(vps.getDeadTime().getTime() + ms));
                                    }
                                    vps.setDeadStatus("0");
                                    vps.setRunStatus("1");
                                    mongoTemplate.save(vps);

                                }
                            } finally {
                                lock2.unlock();
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
            }
        }

        return ResponseResult.success();
    }

}
