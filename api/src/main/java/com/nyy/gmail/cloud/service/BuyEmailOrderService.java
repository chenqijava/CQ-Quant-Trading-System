package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.model.dto.BuyEmailOrderReqDto;
import com.nyy.gmail.cloud.model.dto.ExportAccountReqDto;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.model.dto.ResetPlatformReqDto;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.utils.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nyy.gmail.cloud.service.GetCodeService.filterHtmlUsingRegex;

@Slf4j
@Service
public class BuyEmailOrderService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountPlatformService accountPlatformService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BuyEmailOrderRepository buyEmailOrderRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private AccountService accountService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    @Qualifier("receiveCodeExecutor")
    private Executor taskExecutor;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private BuyEmailOrderDetailRepository buyEmailOrderDetailRepository;

    @Autowired
    private EmailReceiveRecordRepository emailReceiveRecordRepository;

    @Autowired
    private BalanceDetailRepository balanceDetailRepository;

    @Autowired
    private ParamsService paramsService;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private CommissionRecordRepository commissionRecordRepository;

    @Autowired
    private AccountExportRecordRepository accountExportRecordRepository;

    public static final String outOfStockEmail = "outofstock@gmail.com";

    private static final int maxReleaseCountPerHour = 60;

    public String buy(BuyEmailOrderReqDto reqDTO, String userID) {
        // 先创建订单
        if (StringUtils.isEmpty(reqDTO.getPlatformId()) || reqDTO.getBuyNum() == null || reqDTO.getBuyNum() <= 0 || reqDTO.getBuyNum().compareTo(99999) > 0) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }

        AccountPlatform platform = accountPlatformService.findPriceById(reqDTO.getPlatformId(), userID);
        // 不可见不能购买
        if (platform == null || !platform.getDisplayStatus()) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        BigDecimal totalPrice = platform.getPrice().multiply(new BigDecimal(reqDTO.getBuyNum()));
        // 判断余额
        User user = userRepository.findOneByUserID_(userID);
        if (user == null) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (user.getBalance().compareTo(totalPrice.add(user.getFrozenBalance())) < 0) {
            throw new CommonException(ResultCode.USER_BALANCE_INSUFFICIENT); // 余额不足
        }

        // 判断库存
        long l = accountRepository.countNoUsePlatformStock(null, platform.get_id(), null);
        if (l < reqDTO.getBuyNum()) {
            throw new CommonException(ResultCode.OUT_OF_STOCK);
        }

        BuyEmailOrder order = new BuyEmailOrder();
        order.setPlatformId(reqDTO.getPlatformId());
        order.setBuyNum(reqDTO.getBuyNum());
        order.setUserID(userID);
        order.setStatus(BuyEmailOrderStatus.doing.getCode());
        order.setTotalPrice(totalPrice);
        order.setOrderNo(UUIDUtils.getBuyEmailOrderNo());
        order.setFinishNum(0);
        order.setCreateTime(new Date());
        order.setPayPrice(BigDecimal.ZERO);
        order.setPlatformName(platform.getName());
        order.setUnitPrice(platform.getPrice());
        order.setType("v1");
        buyEmailOrderRepository.save(order);

        RLock lock2 = redissonClient.getLock("BuyEmailOrder:" + order.getOrderNo());
        try {
            if (lock2.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    // 冻结金额
                    RLock lock = redissonClient.getLock(userID + "-charge");
                    boolean flag = true;
                    if (lock.tryLock(30, TimeUnit.SECONDS)) {
                        try {
                            user = userRepository.findOneByUserID_(userID);
                            if (user != null) {
                                try {
                                    user.setFrozenBalance(user.getFrozenBalance().add(order.getTotalPrice()));
                                    userRepository.updateUser(user);
                                    flag = false;
                                } catch (OptimisticLockingFailureException oe) {
                                    log.error("下单购买邮件订单2：{} 收到通知，更新乐观锁异常：{}", user.getUserID(), order.getOrderNo(), oe);
                                }
                            }
                        } finally {
                            lock.unlock();
                        }
                    }

                    if (flag) {
                        order = buyEmailOrderRepository.findByIdAndUserID(order.get_id(), userID);
                        order.setStatus(BuyEmailOrderStatus.failed.getCode());
                        order.setFailReason("冻结余额失败");
                        buyEmailOrderRepository.update(order);
                        return order.getOrderNo();
                    }

                    // 创建导出邮箱任务（去导出邮箱任务中创建子订单和退款）
                    try {
                        ExportAccountReqDto exportAccountReqDto = new ExportAccountReqDto();
                        exportAccountReqDto.setPlatformId(reqDTO.getPlatformId());
                        exportAccountReqDto.setExportType("origin");
                        exportAccountReqDto.setCount(reqDTO.getBuyNum());
                        exportAccountReqDto.setOrderId(order.get_id());
                        accountService.exportAccount(exportAccountReqDto, Constants.ADMIN_USER_ID);
                    } catch (Exception e) {
                        // 冻结金额
                        try {
                            order = buyEmailOrderRepository.findByIdAndUserID(order.get_id(), userID);
                            order.setStatus(BuyEmailOrderStatus.failed.getCode());
                            order.setFailReason("创建导出任务失败");
                            buyEmailOrderRepository.update(order);
                            lock = redissonClient.getLock(userID + "-charge");
                            if (lock.tryLock()) {
                                try {
                                    user = userRepository.findOneByUserID_(userID);
                                    if (user != null) {
                                        try {
                                            user.setFrozenBalance(user.getFrozenBalance().subtract(order.getTotalPrice()));
                                            userRepository.updateUser(user);
                                        } catch (OptimisticLockingFailureException oe) {
                                            log.error("下单购买邮件订单1：{} 收到通知，更新乐观锁异常：{}", user.getUserID(), order.getOrderNo(), oe);
                                        }
                                    }
                                } finally {
                                    lock.unlock();
                                }
                            }
                        } catch (Exception oe) {
                            log.error("下单购买邮件订单失败，回滚失败 {} {} ", user.getUserID(), order.getOrderNo(), oe);
                        }
                    }
                } finally {
                    lock2.unlock();
                }
            }
        } catch (InterruptedException e) {
        }
        for (int i = 0; i < 60; i++) {
            List<BuyEmailOrderDetail> details = buyEmailOrderDetailRepository.findByOrderId(order.get_id());
            if (details != null && !details.isEmpty() && details.size() == reqDTO.getBuyNum()) {
                long count = details.stream().filter(e -> StringUtils.isNotEmpty(e.getAccid())).count();
                if (count == reqDTO.getBuyNum()) {
                    break;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        return order.getOrderNo();
    }

    public PageResult<BuyEmailOrder> findByPagination(Params params, int pageSize, int page, String userID) {
        return buyEmailOrderRepository.findByPagination(params, pageSize, page);
    }

    public PageResult<BuyEmailOrderDetail> findDetailsByPagination(Params params, int pageSize, int page, String userID) {
        return buyEmailOrderDetailRepository.findByPagination(params, pageSize, page);
    }

    public void release(String id, String userID) {
        BuyEmailOrderDetail detail = buyEmailOrderDetailRepository.findByIdAndUserID(id, userID);

        // 查询最近1小时的释放记录
        List<BuyEmailOrderDetail> details = buyEmailOrderDetailRepository.findByUserIDAndRecentOneHourRelease(userID);
        if (details.size() >= maxReleaseCountPerHour) {
            throw new CommonException(ResultCode.RELEASE_QUICK);
        }

        if (detail != null && detail.getStatus().equals(BuyEmailDetailOrderStatus.Waiting.getCode())) {
            for (int i = 0; i < 10; i++) {
                BuyEmailOrder order = buyEmailOrderRepository.findByIdAndUserID(detail.getOrderId(), userID);
                RLock lock = redissonClient.getLock("BuyEmailOrder:" + order.getOrderNo());
                if (lock.tryLock()) {
                    try {
                        detail = buyEmailOrderDetailRepository.findByIdAndUserID(id, userID);
                        if (detail != null && detail.getStatus().equals(BuyEmailDetailOrderStatus.Waiting.getCode())) {
                            detail.setStatus(BuyEmailDetailOrderStatus.Released.getCode());
                            detail.setUpdateTime(new Date());
                            buyEmailOrderDetailRepository.update(detail);
                            releaseBalance(userID, detail.getPrice(), detail.getOrderDetailNo());

                            ResetPlatformReqDto resetPlatformReqDto = new ResetPlatformReqDto();
                            resetPlatformReqDto.setPlatformId(order.getPlatformId());
                            resetPlatformReqDto.setAccId(detail.getAccid());
                            accountService.resetPlatform(resetPlatformReqDto, Constants.ADMIN_USER_ID);
                        }
                        break;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    private void releaseBalance(String userID, BigDecimal balance, String orderNo) {
        RLock lock = redissonClient.getLock(userID + "-charge");
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    User user = userRepository.findOneByUserID_(userID);
                    if (user != null) {
                        try {
                            user.setFrozenBalance(user.getFrozenBalance().subtract(balance));
                            userRepository.updateUser(user);
                        } catch (OptimisticLockingFailureException oe) {
                            log.error("下单购买邮件订单3：{} 收到通知，更新乐观锁异常：{}", user.getUserID(), orderNo, oe);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
        }
    }

    public void receiveEmail() {
        List<BuyEmailOrder> orderList1 = buyEmailOrderRepository.findByStatus(BuyEmailOrderStatus.doing.getCode());
        List<String> dealIds = new ArrayList<>();
        for (BuyEmailOrder order : orderList1) {
            asyncProcessOrder(order);
            dealIds.add(order.get_id());
        }
        List<String> orderIds = buyEmailOrderDetailRepository.findReceiveEmailOrder();

        orderIds = orderIds.stream().filter(e -> !dealIds.contains(e)).toList();

        List<BuyEmailOrder> orderList = buyEmailOrderRepository.findByIdIn(orderIds);
        for (BuyEmailOrder order : orderList) {
            asyncProcessOrder(order);
        }
    }

    private void asyncProcessOrder(BuyEmailOrder order) {
        taskExecutor.execute(() -> {
            RLock lock = redissonClient.getLock("BuyEmailOrder:" + order.getOrderNo());
            if (lock.tryLock()) {
                try {
                    List<BuyEmailOrderDetail> details = buyEmailOrderDetailRepository.findByOrderId(order.get_id());
                    if (details.size() != order.getBuyNum()) {
                        GroupTask groupTask = groupTaskRepository.findOneByOrderId(order.get_id());
                        if (groupTask != null && groupTask.getPublishStatus().equals("success")) {
                            // 库存不足，剩下的detail创建并释放掉
                            String emailExpireTime = paramsService.getParams("account.emailExpireTime", null, null).toString();
                            for (int i = 0; i < order.getBuyNum() - details.size(); i++) {
                                BuyEmailOrderDetail detail = new BuyEmailOrderDetail();
                                detail.setEmail(outOfStockEmail);
                                detail.setOrderDetailNo(UUIDUtils.getBuyEMailOrderDetailNo(order.getOrderNo(), i + details.size()));
                                detail.setOrderId(order.get_id());
                                detail.setPrice(order.getUnitPrice());
                                detail.setText("");
                                detail.setStatus(BuyEmailDetailOrderStatus.Waiting.getCode());
                                detail.setCreateTime(new Date());
                                detail.setUpdateTime(new Date());
                                Calendar instance = Calendar.getInstance();
                                instance.add(Calendar.MINUTE, Integer.parseInt(emailExpireTime));
                                detail.setExpireTime(instance.getTime());
                                detail.setPlatformId(order.getPlatformId());
                                detail.setPlatformName(order.getPlatformName());
                                detail.setUserID(order.getUserID());
                                detail.setSubTaskId("");
                                detail.setAccid("");
                                buyEmailOrderDetailRepository.save(detail);
                            }
                        }
                        return;
                    }

                    if (details.stream().anyMatch(e -> e.getStatus().equals(BuyEmailDetailOrderStatus.Waiting.getCode()))) {
                        BigDecimal payPrice = BigDecimal.ZERO;
                        int finishNum = 0;
                        for (BuyEmailOrderDetail detail : details) {
                            if (detail.getStatus().equals(BuyEmailDetailOrderStatus.CodeReceived.getCode()) || detail.getStatus().equals(BuyEmailDetailOrderStatus.Expired.getCode())) {
                                payPrice = payPrice.add(detail.getPrice());
                                finishNum += 1;
                            }
                        }
                        order.setFinishNum(finishNum);
                        order.setPayPrice(payPrice);
                        buyEmailOrderRepository.update(order);
                    } else {
                        BigDecimal payPrice = BigDecimal.ZERO;
                        int finishNum = 0;
                        for (BuyEmailOrderDetail detail : details) {
                            if (detail.getStatus().equals(BuyEmailDetailOrderStatus.CodeReceived.getCode()) || detail.getStatus().equals(BuyEmailDetailOrderStatus.Expired.getCode())) {
                                payPrice = payPrice.add(detail.getPrice());
                                finishNum += 1;
                            }
                        }

                        order.setFinishNum(finishNum);
                        order.setPayPrice(payPrice);
                        order.setStatus(BuyEmailOrderStatus.finish.getCode());
                        buyEmailOrderRepository.update(order);
                    }

                    for (BuyEmailOrderDetail detail : details) {
                        if (detail.getStatus().equals(BuyEmailDetailOrderStatus.Waiting.getCode()) && detail.getEmail().equals(outOfStockEmail)) {
                            detail.setStatus(BuyEmailDetailOrderStatus.Released.getCode());
                            detail.setUpdateTime(new Date());
                            buyEmailOrderDetailRepository.update(detail);

                            releaseBalance(detail.getUserID(), detail.getPrice(), detail.getOrderDetailNo());
                            continue;
                        }
                        if (detail.getStatus().equals(BuyEmailDetailOrderStatus.Waiting.getCode())) {
                            Account account = accountRepository.findById(detail.getAccid());
                            if (account == null || account.getOnlineStatus().equals(AccountOnlineStatus.OFFLINE.getCode())) {
                                detail.setStatus(BuyEmailDetailOrderStatus.Released.getCode());
                                detail.setUpdateTime(new Date());
                                buyEmailOrderDetailRepository.update(detail);

                                releaseBalance(detail.getUserID(), detail.getPrice(), detail.getOrderDetailNo());
                                continue;
                            }
                        }

                        AccountPlatform platform = accountPlatformService.findPriceById(detail.getPlatformId(), detail.getUserID());
                        if (platform == null) { continue; }
                        if (detail.getStatus().equals(BuyEmailDetailOrderStatus.CodeReceived.getCode()) || detail.getStatus().equals(BuyEmailDetailOrderStatus.Waiting.getCode())) {
                            if (detail.getExpireTime().compareTo(new Date()) >= 0) {
                                SubTask subTask = subTaskRepository.findById(detail.getSubTaskId());
                                subTask.setTmpId("2");
                                subTaskRepository.save(subTask);

                                List<Message> messageList = messageRepository.findByAccIdAndCreateTimeGatherThan(subTask.getAccid(), subTask.getCreateTime());
                                messageList = messageList.stream().filter(e -> {
                                    if (StringUtils.isEmpty(platform.getEmailFrom())) {
                                        return true;
                                    }
                                    String[] split = platform.getEmailFrom().split(",", -1);
                                    for (String p : split) {
                                        if (e.getSender().contains(p)) {
                                            return true;
                                        }
                                    }
                                    return false;
                                }).sorted((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime())).toList();

                                if (!messageList.isEmpty()) {
                                    Message first = messageList.getFirst();
                                    if (StringUtils.isNotEmpty(platform.getPattern())) {
                                        Pattern pattern = Pattern.compile(platform.getPattern());
                                        Matcher matcher = pattern.matcher(platform.getPattern().contains("http") ? first.getText() : filterHtmlUsingRegex(first.getText()));
                                        if (matcher.find()) {
                                            if (matcher.groupCount() > 0) {
                                                detail.setText(matcher.group(1));
                                            } else {
                                                detail.setText(matcher.group());
                                            }
                                        }
                                    } else {
                                        String text = filterHtmlUsingRegex(first.getText());
                                        detail.setText(text);
                                    }
                                    String status = detail.getStatus();
                                    detail.setStatus(BuyEmailDetailOrderStatus.CodeReceived.getCode());
                                    detail.setReceiveTime(StringUtils.isEmpty(first.getTimestamp()) ? new Date() : new Date(Long.parseLong(first.getTimestamp())));
                                    detail.setUpdateTime(new Date());
                                    buyEmailOrderDetailRepository.update(detail);
                                    if (status.equals(BuyEmailDetailOrderStatus.Waiting.getCode())) {
                                        BalanceDetail balanceDetail = balanceDetailRepository.findByEmailOrderNo(detail.getOrderDetailNo());
                                        if (balanceDetail == null) {
                                            useBalance(detail.getUserID(), detail.getPrice(), detail.getOrderDetailNo(), detail.getPlatformId());
                                        }
                                    }

                                    // 修改ACCOUNT
                                    Account account = accountRepository.findById(subTask.getAccid());
                                    if (account.getRealUsedPlatformIds() == null) {
                                        account.setRealUsedPlatformIds(new ArrayList<>());
                                    }
                                    if (!account.getRealUsedPlatformIds().contains(platform.get_id())) {
                                        account.getRealUsedPlatformIds().add(platform.get_id());
                                        accountRepository.update(account);
                                    }

                                    // 保存记录
                                    EmailReceiveRecord record = emailReceiveRecordRepository.findOneBySubTaskIdAndAccId(subTask.get_id(), subTask.getAccid());
                                    if (record == null) {
                                        record = new EmailReceiveRecord();
                                        record.setEmail(account.getEmail());
                                        record.setText(detail.getText());
                                        record.setCreateTime(new Date());
                                        record.setAllText(first.getText());
                                        record.setPlatformId(platform.get_id());
                                        record.setPlatformName(platform.getName());
                                        record.setAccId(account.get_id());
                                        record.setUserID(account.getUserID());
                                        record.setReceiveTime(StringUtils.isEmpty(first.getTimestamp()) ? new Date() : new Date(Long.parseLong(first.getTimestamp())));
                                        record.setSubTaskId(subTask.get_id());
                                        emailReceiveRecordRepository.save(record);
                                    }
                                }
                            } else {
                                SubTask subTask = subTaskRepository.findById(detail.getSubTaskId());
                                subTask.setTmpId("1");
                                subTaskRepository.save(subTask);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.info("检查接受邮件错误： {}", e.getMessage());
                }
                finally {
                    lock.unlock();
                }
            }
        });
    }

    private void useBalance(String userID, BigDecimal price, String orderDetailNo, String platformId) {
        RLock lock = redissonClient.getLock(userID + "-charge");
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    User user = userRepository.findOneByUserID_(userID);
                    if (user != null) {
                        try {
                            BigDecimal afterValue = user.getBalance().subtract(price);
                            user.setBalance(afterValue);
                            user.setFrozenBalance(user.getFrozenBalance().subtract(price));
                            userRepository.updateUser(user);

                            balanceDetailRepository.addUserBill("下单扣款",
                                    user.getUserID(), price, user.getBalance(), user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.BUY_ORDER, orderDetailNo);

                            // 分佣
                            String referrer = user.getReferrer();
                            if (StringUtils.isNotEmpty(referrer)) {
                                String[] split = referrer.split(",");
                                BigDecimal restPrice = price;
                                String fromUserID = userID;
                                for (int i = split.length - 1; i >= 0; i--) {
                                    String ref = split[i];
                                    AccountPlatform platform = accountPlatformService.findPriceById(platformId, ref);
                                    BigDecimal refPrice = restPrice.subtract(platform.getPrice());
                                    if (refPrice.compareTo(BigDecimal.ZERO) > 0) {
                                        RLock lock2 = redissonClient.getLock(ref + "-charge");
                                        if (lock2.tryLock()) {
                                            try {
                                                User user2 = userRepository.findOneByUserID_(ref);
                                                if (user2 != null) {
                                                    BigDecimal afterValue2 = user2.getBalance().add(refPrice);
                                                    user2.setBalance(afterValue2);
                                                    userRepository.updateUser(user2);

                                                    balanceDetailRepository.addUserBill(fromUserID + " 返佣",
                                                            user2.getUserID(), refPrice, user2.getBalance(), user2.getName(), BillExpenseTypeEnums.IN, BillCateTypeEnums.COMMISSION);

                                                    CommissionRecord commissionRecord = commissionRecordRepository.findByUserIDAndFromID(ref, userID);
                                                    if (commissionRecord == null) {
                                                        commissionRecord = new CommissionRecord();
                                                        commissionRecord.setAmount(refPrice);
                                                        commissionRecord.setFromUserID(userID);
                                                        commissionRecord.setCreateTime(new Date());
                                                        commissionRecord.setUserID(ref);
                                                    } else {
                                                        commissionRecord.setAmount(commissionRecord.getAmount().add(refPrice));
                                                    }
                                                    commissionRecordRepository.save(commissionRecord);
                                                }
                                            } finally {
                                                lock2.unlock();
                                            }
                                        }
                                    }
                                    restPrice = platform.getPrice();
                                    fromUserID = ref;
                                }
                            }
                        } catch (Exception oe) {
                            log.error("下单购买邮件订单3：{} 收到通知，更新乐观锁异常：{}", user.getUserID(), orderDetailNo, oe);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            log.error("获取锁异常", userID, orderDetailNo, e);
        }
    }

    public void checkExpired() {
        List<BuyEmailOrder> orderList = buyEmailOrderRepository.findByStatus(BuyEmailOrderStatus.doing.getCode());
        for (BuyEmailOrder order : orderList) {
            taskExecutor.execute(() -> {
                RLock lock = redissonClient.getLock("BuyEmailOrder:" + order.getOrderNo());
                if (lock.tryLock()) {
                    try {
                        List<BuyEmailOrderDetail> details = buyEmailOrderDetailRepository.findByOrderId(order.get_id());
                        for (BuyEmailOrderDetail detail : details) {
                            if (detail.getStatus().equals(BuyEmailDetailOrderStatus.Waiting.getCode()) && detail.getExpireTime().compareTo(new Date()) <= 0) {
                                detail.setStatus(BuyEmailDetailOrderStatus.Expired.getCode());
                                detail.setUpdateTime(new Date());
                                buyEmailOrderDetailRepository.update(detail);
                                BalanceDetail balanceDetail = balanceDetailRepository.findByEmailOrderNo(detail.getOrderDetailNo());
                                if (balanceDetail == null) {
                                    useBalance(detail.getUserID(), detail.getPrice(), detail.getOrderDetailNo(), detail.getPlatformId());

                                    ResetPlatformReqDto resetPlatformReqDto = new ResetPlatformReqDto();
                                    resetPlatformReqDto.setPlatformId(order.getPlatformId());
                                    resetPlatformReqDto.setAccId(detail.getAccid());
                                    accountService.resetPlatform(resetPlatformReqDto, Constants.ADMIN_USER_ID);
                                }
//                                releaseBalance(detail.getUserID(), detail.getPrice(), detail.getOrderDetailNo());
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }
    }

    public List<BuyEmailOrderDetail> openDetails(String orderNo, String userID) {
        BuyEmailOrder order = buyEmailOrderRepository.findByOrderNo(orderNo, userID);
        if (order == null) {
            return new ArrayList<>();
        }
        List<BuyEmailOrderDetail> details = buyEmailOrderDetailRepository.findByOrderId(order.get_id());
        return details;
    }

    public String buyV2(BuyEmailOrderReqDto reqDTO, String userID) {
        // 先创建订单
        if (StringUtils.isEmpty(reqDTO.getPlatformId()) || reqDTO.getBuyNum() == null || reqDTO.getBuyNum() <= 0 || reqDTO.getBuyNum().compareTo(99999) > 0) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }

        AccountPlatform platform = accountPlatformService.findPriceById(reqDTO.getPlatformId(), userID);
        // 不可见不能购买
        if (platform == null || !platform.getDisplayStatus()) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        BigDecimal totalPrice = platform.getPrice().multiply(new BigDecimal(reqDTO.getBuyNum()));
        // 判断余额
        User user = userRepository.findOneByUserID_(userID);
        if (user == null) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (user.getBalance().compareTo(totalPrice.add(user.getFrozenBalance())) < 0) {
            throw new CommonException(ResultCode.USER_BALANCE_INSUFFICIENT); // 余额不足
        }

        // 判断库存
        long l = accountRepository.countNoUsePlatformStock(null, platform.get_id(), null);
        if (l < reqDTO.getBuyNum()) {
            throw new CommonException(ResultCode.OUT_OF_STOCK);
        }

        BuyEmailOrder order = new BuyEmailOrder();
        order.setPlatformId(reqDTO.getPlatformId());
        order.setBuyNum(reqDTO.getBuyNum());
        order.setUserID(userID);
        order.setStatus(BuyEmailOrderStatus.finish.getCode());
        order.setTotalPrice(totalPrice);
        order.setOrderNo(UUIDUtils.getBuyEmailOrderNo());
        order.setFinishNum(reqDTO.getBuyNum());
        order.setCreateTime(new Date());
        order.setPayPrice(totalPrice);
        order.setPlatformName(platform.getName());
        order.setUnitPrice(platform.getPrice());
        order.setType("v2");
        buyEmailOrderRepository.save(order);

        RLock lock2 = redissonClient.getLock("BuyEmailOrder:" + order.getOrderNo());
        try {
            if (lock2.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    ExportAccountReqDto exportAccountReqDto = new ExportAccountReqDto();
                    exportAccountReqDto.setPlatformId(reqDTO.getPlatformId());
                    exportAccountReqDto.setExportType("origin");
                    exportAccountReqDto.setCount(reqDTO.getBuyNum());
                    exportAccountReqDto.setOrderId(order.get_id());
                    GroupTask groupTask = accountService.exportAccount(exportAccountReqDto, Constants.ADMIN_USER_ID);
                    String baseUrl = paramsService.getParams("webConfig.baseUrl", null, null).toString();
                    for (int j = 0; j < 60; j++) {
                        if (groupTask != null) {
                            Thread.sleep(1000);
                            String recordId = groupTask.getParams().get("recordId").toString();
                            AccountExportRecord record = accountExportRecordRepository.findById(recordId);
                            if (record != null && !StringUtils.isEmpty(record.getFilepath())) {
                                useBalance(userID, totalPrice, platform.get_id());

                                String downloadUrl = baseUrl + "/api/consumer/res/download/" + record.getFilepath();
                                buyEmailOrderRepository.saveDownloadUrl(order.getOrderNo(), downloadUrl);
                                return downloadUrl;
                            }
                        } else {
                            break;
                        }
                    }
                } finally {
                    lock2.unlock();
                }
            }
        } catch (InterruptedException e) {
        }
        throw new CommonException(ResultCode.CREATE_ORDER_FAIL);
    }

    private void useBalance(String userID, BigDecimal totalPrice, String platformId) {
        User user = userRepository.findOneByUserID_(userID);
        if (user != null) {
            try {
                BigDecimal afterValue = user.getBalance().subtract(totalPrice);
                user.setBalance(afterValue);
                userRepository.updateUser(user);

                balanceDetailRepository.addUserBill("下单扣款",
                        user.getUserID(), totalPrice, user.getBalance(), user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.BUY_ORDER, "");

                // 分佣
                String referrer = user.getReferrer();
                if (StringUtils.isNotEmpty(referrer)) {
                    String[] split = referrer.split(",");
                    BigDecimal restPrice = totalPrice;
                    String fromUserID = userID;
                    for (int i = split.length - 1; i >= 0; i--) {
                        String ref = split[i];
                        AccountPlatform platform = accountPlatformService.findPriceById(platformId, ref);
                        BigDecimal refPrice = restPrice.subtract(platform.getPrice());
                        if (refPrice.compareTo(BigDecimal.ZERO) > 0) {
                            RLock lock3 = redissonClient.getLock(ref + "-charge");
                            if (lock3.tryLock()) {
                                try {
                                    User user2 = userRepository.findOneByUserID_(ref);
                                    if (user2 != null) {
                                        BigDecimal afterValue2 = user2.getBalance().add(refPrice);
                                        user2.setBalance(afterValue2);
                                        userRepository.updateUser(user2);

                                        balanceDetailRepository.addUserBill(fromUserID + " 返佣",
                                                user2.getUserID(), refPrice, user2.getBalance(), user2.getName(), BillExpenseTypeEnums.IN, BillCateTypeEnums.COMMISSION);

                                        CommissionRecord commissionRecord = commissionRecordRepository.findByUserIDAndFromID(ref, userID);
                                        if (commissionRecord == null) {
                                            commissionRecord = new CommissionRecord();
                                            commissionRecord.setAmount(refPrice);
                                            commissionRecord.setFromUserID(userID);
                                            commissionRecord.setCreateTime(new Date());
                                            commissionRecord.setUserID(ref);
                                        } else {
                                            commissionRecord.setAmount(commissionRecord.getAmount().add(refPrice));
                                        }
                                        commissionRecordRepository.save(commissionRecord);
                                    }
                                } finally {
                                    lock3.unlock();
                                }
                            }
                        }
                        restPrice = platform.getPrice();
                        fromUserID = ref;
                    }
                }
            } catch (Exception oe) {
                log.error("下单购买邮件订单3：{} 收到通知，更新乐观锁异常：{}", user.getUserID(), "", oe);
            }
        }
    }
}
