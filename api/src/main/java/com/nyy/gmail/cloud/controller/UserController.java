package com.nyy.gmail.cloud.controller;

import com.alibaba.fastjson.JSON;
import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.Check2FA;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationBuilder;
import com.nyy.gmail.cloud.common.pagination.MongoPaginationHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.AccountGroup;
import com.nyy.gmail.cloud.entity.mongo.BalanceDetail;
import com.nyy.gmail.cloud.entity.mongo.Role;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.enums.AccountGroupTypeEnums;
import com.nyy.gmail.cloud.enums.BillCateTypeEnums;
import com.nyy.gmail.cloud.enums.BillExpenseTypeEnums;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.model.vo.UserSaveVO;
import com.nyy.gmail.cloud.repository.mongo.BalanceDetailRepository;
import com.nyy.gmail.cloud.repository.mongo.RoleRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.service.AccountGroupService;
import com.nyy.gmail.cloud.service.UserService;
import com.nyy.gmail.cloud.utils.ByteUtil;
import com.nyy.gmail.cloud.utils.MD5Util;
import com.nyy.gmail.cloud.utils.TokenUtils;
import com.nyy.gmail.cloud.utils.UUIDUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @author liang sishuai
 * @version 1.0
 * @date 2021-09-16
 */

@Slf4j
@Component
@RestController
@RequestMapping("/api/consumer/user/")
public class UserController {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest request;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private BalanceDetailRepository balanceDetailRepository;

    @Resource
    private MongoPaginationHelper mongoPaginationHelper;

    @Resource
    private AccountGroupService accountGroupService;

    @Autowired
    private RoleRepository roleRepository;

    @RequiredPermission({MenuType.adminUser, MenuType.ownerUser})
    @PostMapping("card/save")
    public Result<UserSaveVO> save(@RequestBody User user) {
        String userID = (String) request.getSession().getAttribute("userID");
        try {
            if (user.getUserID().contains(",")) {
                throw new CommonException("登录ID中不能包含符号");
            }
            if (user.getUserID() != null && !user.getUserID().equals("")) {
                user.setUserID(user.getUserID().trim());
            }
//            if (user.getPassword() != null && !user.getPassword().equals("")) {
//                user.setPassword(user.getPassword().trim());
//                MessageDigest md = MessageDigest.getInstance("MD5");
//                md.update(String.valueOf(new Date().getTime()).getBytes());
//                String session = ByteUtil.toHexString(md.digest());
//                user.setSession(session);
//            }
            if (user.getName() != null && !user.getName().equals("")) {
                user.setName(user.getName().trim());
            }
            if (user.getSocks5Area() != null && !user.getSocks5Area().equals("")) {
                user.setSocks5Area(user.getSocks5Area().trim());
            }
            if (StringUtils.isNotBlank(user.getRole())) {
                user.setRole(user.getRole().trim());
            } else {
                User opUser = userRepository.findOneByUserID(userID);
                user.setRole(opUser.getRole().trim());
            }
            if (StringUtils.isBlank(user.getToken())) {
                user.setToken(TokenUtils.generate());
            }

            // 更新用户
            if (StringUtils.isNotBlank(user.get_id())) {
                //更新
                User tmp = userRepository.findOneById(user.get_id());
                if (tmp == null || !tmp.getUserID().equals(user.getUserID())) {
                    throw new CommonException(ResultCode.PARAMS_IS_INVALID);
                }
                if (StringUtils.isEmpty(user.getUserApiKey())) {
                    user.setUserApiKey(UUIDUtils.get32UUId());
                }
                user.setPassword(null);
                if (userID.equals("admin") || userID.equals(tmp.getUserID()) || tmp.getCreateUserID().equals(userID)) {
                    //userRepository.updateUser(user);
                    if (StringUtils.isNotBlank(user.getNewPassword())) {
                        //userRepository.updateUser(user);
                        String pwd = MD5Util.MD5(MD5Util.MD5(user.getUserID()) + user.getNewPassword());
                        user.setPassword(pwd);
                    }
                    Role role = roleRepository.findRoleById(user.getRole());
                    if (!role.getUserID().equals(userID)) {
                        user.setRole(null);
                    }
                    userRepository.findOneAndUpdateBySave(user.getUserID(), user.getPassword(), null, user.getName(), user.getSocks5Area(), user.getRole(), user.getBotId(), user.getToken(), user.getUserApiKey());
                    UserSaveVO userSaveVO = new UserSaveVO();
                    userSaveVO.set_id(user.get_id());
                    return ResponseResult.success(userSaveVO);
                } else {
                    throw new CommonException(ResultCode.NO_PERMISSION);
                }
            } else {
                //新建
                String pwd = MD5Util.MD5(MD5Util.MD5(user.getUserID()) + user.getPassword());
                user.setPassword(pwd);
                user.setCreateUserID(userID);
                // user.setOrderStatus("free");
                user.setCreateTime(new Date());
                user.setBalance(new BigDecimal(0));
                user.setUserApiKey(UUIDUtils.get32UUId());
                if (userID.equals(Constants.ADMIN_USER_ID)) {
                    user.setOpenRecharge("open");
                }

                User opUser = userRepository.findOneByUserID(userID);
                user.setReferrer(StringUtils.isEmpty(opUser.getReferrer()) ? opUser.getUserID() : opUser.getReferrer() + "," + opUser.getUserID());
                // 角色判断
//                Role role = roleRepository.findRoleById(user.getRole());
//                if (!role.getUserID().equals(userID)) {
//                    throw new CommonException(ResultCode.NO_PERMISSION);
//                }
                userRepository.saveUser(user);

                if (StringUtils.isNotEmpty(user.getReferrer())) {
                    String[] split = user.getReferrer().split(",");
                    for (int i = split.length - 1; i >= 0; i--) {
                        opUser = userRepository.findOneByUserID(split[i]);
                        if (opUser != null) {
                            if (opUser.getReferrerCount() == null) {
                                opUser.setReferrerCount(0);
                            }
                            opUser.setReferrerCount(opUser.getReferrerCount() + 1);
                            userRepository.updateUser(opUser);
                        }
                    }
                }

                UserSaveVO userSaveVO = new UserSaveVO();
                userSaveVO.set_id(user.get_id());

                return ResponseResult.success(userSaveVO);
            }
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new CommonException("用户名或者ID重复");
        } catch (Exception e) {
            Result<UserSaveVO> result = ResponseResult.failure(ResultCode.ERROR, null);
            result.setMessage(e.getMessage());
            return result;
        }
    }

    private void addDefaultGroup(String userID){
        AccountGroup accountGroup = new AccountGroup();
        accountGroup.setGroupName(AccountGroupTypeEnums.DEFAULT.getDescription());
        accountGroupService.save(accountGroup,userID, AccountGroupTypeEnums.DEFAULT);
    }

    @RequiredPermission(MenuType.adminUser)
    @GetMapping("getAllUser")
    public Result<List<User>> get() {
        String userID = (String) request.getSession().getAttribute("userID");
        List<String> userIDs = new ArrayList<String>();
        if (!userID.equals("admin")) {
            List<User> userList = userRepository.findUserByCreateUserID(userID);
            for (User user_ : userList) {
                userIDs.add(user_.getUserID());
            }
            userIDs.add(userID);
        } else {
            List<User> userByCreateUserID = userRepository.findUserByCreateUserID(userID);
            userByCreateUserID.forEach(e -> e.setPassword(""));
            return ResponseResult.success(userByCreateUserID);
        }

        List<User> userList = userRepository.findByUserIDs(userIDs);
        userList.forEach(e -> {
            e.setPassword("");
            e.setSecret("");
            e.setReferrer("");
        });
        Result<List<User>> result = ResponseResult.success(userList);
        return result;
    }

    @GetMapping("card/{_id}")
    public Result<User> get(@PathVariable String _id) {
        User user = userRepository.findOneById(_id);
        if (user != null) {
            user.setPassword("");
            user.setSecret("");
            user.setReferrer("");
        }
        Result<User> result = ResponseResult.success(user);
        return result;
    }

    @RequiredPermission({MenuType.adminUser, MenuType.ownerUser})
    @PostMapping("{pageSize}/{page}")
    public Result<PageResult<User>> list(@PathVariable("pageSize") int pageSize, @PathVariable("page") int page, @RequestBody(required = false) UserListDTO userListDTO) {
        String userID = (String) request.getSession().getAttribute("userID");

        if (userListDTO == null) {
            userListDTO = new UserListDTO();
        }
        if (userID != null && !userID.equals("admin")) {
            userListDTO.setCreateUserID(userID);
        }

        if (userListDTO != null && userListDTO.getFilters() != null) {
            Map<String, String> filtersMap = userListDTO.getFilters();
            String name = filtersMap.get("name");
            String _userID = filtersMap.get("userID");

            if (StringUtils.isNotEmpty(name)) {
                userListDTO.setName(name);
            }
            if (StringUtils.isNotEmpty(_userID)) {
                userListDTO.setUserID(_userID);
            }

        }

        PageResult<User> pageResult = userService.findUserByPagination(userListDTO, pageSize, page);//按id倒排序，未做到populate
        pageResult.getData().forEach(e -> {
            e.setPassword("");
            e.setSecret("");
            e.setReferrer("");
        });
        Result<PageResult<User>> result = ResponseResult.success(pageResult);
        return result;
    }

    @RequiredPermission(MenuType.adminUser)
    @PostMapping("editPassword")
    public Result<String> editPwd(@RequestBody UserEditPwdDTO userEditPwdDTO) {
        String userID = (String) request.getSession().getAttribute("userID");
        try {
            String pwd = MD5Util.MD5(MD5Util.MD5(userID) + userEditPwdDTO.getPassword().trim());
            String session = MD5Util.MD5(String.valueOf(new Date().getTime()));

            userRepository.findAndUpdateByIdsUserID(userEditPwdDTO.getIds(), userID, pwd, session);
        } catch (Exception e) {
            Result<String> result = ResponseResult.failure(ResultCode.ERROR, e.getMessage());
            return result;
        }

        Result<String> result = ResponseResult.success(null);
        return result;
    }

    @RequiredPermission(MenuType.adminUser)
    @PostMapping("charge")
    @Check2FA
    public Result<String> charge(@RequestBody UserChargeDTO param) {
        log.error("charge params:{}", JSON.toJSONString(param));

        try {
            if (param == null || param.getChargeValue() == null) {
                throw new CommonException("参数错误");
            }
            if (param.getChargeValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CommonException("金额必须大于0");
            }
            if (param.getChargeValue().compareTo(BigDecimal.valueOf(10_000_000)) > 0) {
                throw new CommonException("金额必须小于10000000");
            }
            if (param.getChargeValue().compareTo(new BigDecimal("0")) <= 0) {
                log.error("charge params:{}", JSON.toJSONString(param));
                throw new CommonException("参数错误");
            }
            if (CollectionUtils.isEmpty(param.getIds())) {
                throw new CommonException("请选择用户");
            }
            String userID = Session.currentSession().userID;

            RLock lock = redissonClient.getLock(userID + "-charge");
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    User user = userRepository.findOneByUserID(userID);

                    // google验证器
                    if (StringUtils.isEmpty(user.getSecret())) {
                        throw new CommonException("请先绑定google验证器");
                    }

                    if (!Constants.ADMIN_USER_ID.equals(user.getUserID())
                            && user.getBalance().compareTo(param.getChargeValue().multiply(BigDecimal.valueOf(param.getIds().size()))) < 0) {
                        return ResponseResult.failure(0, "余额不足无法充值");
                    }
                    for (String id : param.getIds()) {
                        User one = userRepository.findOneById(id);
                        if (one != null) {
                            if (one.getBalance().add(param.getChargeValue()).compareTo(BigDecimal.valueOf(100_000_000)) > 0) {
                                return ResponseResult.failure(0, "余额不能大于1亿");
                            }
                        }
                    }

                    List<String> users = new ArrayList<>();

                    for (String id : param.getIds()) {
                        User one = userRepository.findOneById(id);
                        if (one != null) {
                            users.add(one.getUserID());
                            one.setBalance(one.getBalance().add(param.getChargeValue()));
                            one.setTotalRechargeBalance(one.getTotalRechargeBalance().add(param.getChargeValue()));
                            userRepository.updateUser(one);

                            user.setBalance(user.getBalance().subtract(param.getChargeValue()));
                            userRepository.updateUser(user);
                            balanceDetailRepository.addUserBill("充值 " + param.getChargeValue().toPlainString(),
                                    one.getUserID(), param.getChargeValue(), one.getBalance(), one.getName(), BillExpenseTypeEnums.IN, BillCateTypeEnums.CHARGE_OFFLINE);
                        }
                    }

                    BigDecimal sumDeltaValue = param.getChargeValue().multiply(BigDecimal.valueOf(users.size()));
                    balanceDetailRepository.addUserBill("为用户" + String.join(",", users) + " 充值 " + param.getChargeValue().toPlainString(),
                            userID, sumDeltaValue, user.getBalance(), user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.CHARGE_OFFLINE);

                } finally {
                    lock.unlock();
                }
            }
            return ResponseResult.success(null);
        } catch (Exception e) {
            log.error("charge error", e);
            return ResponseResult.failure(ResultCode.ERROR, e.getMessage());
        }
    }

    @RequiredPermission(MenuType.adminUser)
    @PostMapping("chargeSendEmail")
    @Check2FA
    public Result<String> chargeSendEmail(@RequestBody UserChargeDTO param) {
        log.error("charge params:{}", JSON.toJSONString(param));

        try {
            if (param == null || param.getChargeValue() == null) {
                throw new CommonException("参数错误");
            }
            if (param.getChargeValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CommonException("充值额度必须大于0");
            }
            if (param.getChargeValue().compareTo(BigDecimal.valueOf(10_000_000)) > 0) {
                throw new CommonException("充值额度必须小于10000000");
            }
            if (param.getChargeValue().compareTo(new BigDecimal("0")) <= 0) {
                log.error("charge params:{}", JSON.toJSONString(param));
                throw new CommonException("参数错误");
            }
            if (CollectionUtils.isEmpty(param.getIds())) {
                throw new CommonException("请选择用户");
            }
            String userID = Session.currentSession().userID;

            RLock lock = redissonClient.getLock(userID + "-charge");
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    User user = userRepository.findOneByUserID(userID);

                    // google验证器
                    if (StringUtils.isEmpty(user.getSecret())) {
                        throw new CommonException("请先绑定google验证器");
                    }

                    if (!Constants.ADMIN_USER_ID.equals(user.getUserID())
                            && user.getRestSendEmailCount().compareTo(param.getChargeValue().multiply(BigDecimal.valueOf(param.getIds().size()))) < 0) {
                        return ResponseResult.failure(0, "群发账户余额不足无法充值");
                    }
                    for (String id : param.getIds()) {
                        User one = userRepository.findOneById(id);
                        if (one != null) {
                            if (one.getRestSendEmailCount().add(param.getChargeValue()).compareTo(BigDecimal.valueOf(100_000_000)) > 0) {
                                return ResponseResult.failure(0, "群发账户余额不能大于1亿");
                            }
                        }
                    }

                    List<String> users = new ArrayList<>();

                    for (String id : param.getIds()) {
                        User one = userRepository.findOneById(id);
                        if (one != null) {
                            users.add(one.getUserID());
                            one.setRestSendEmailCount(one.getRestSendEmailCount().add(param.getChargeValue()));
                            one.setTotalSendEmailCount(one.getTotalSendEmailCount().add(param.getChargeValue()));
                            userRepository.updateUser(one);

                            user.setRestSendEmailCount(user.getRestSendEmailCount().subtract(param.getChargeValue()));
                            userRepository.updateUser(user);
                            balanceDetailRepository.addUserBill("充值群发账户 " + param.getChargeValue().toPlainString(),
                                    one.getUserID(), param.getChargeValue(), one.getRestSendEmailCount(), one.getName(), BillExpenseTypeEnums.IN, BillCateTypeEnums.CHARGE_SEND_EMAIL_COUNT);
                        }
                    }

                    BigDecimal sumDeltaValue = param.getChargeValue().multiply(BigDecimal.valueOf(users.size()));
                    balanceDetailRepository.addUserBill("为用户" + String.join(",", users) + " 充值群发账户 " + param.getChargeValue().toPlainString(),
                            userID, sumDeltaValue, user.getRestSendEmailCount(), user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.CHARGE_SEND_EMAIL_COUNT);

                } finally {
                    lock.unlock();
                }
            }
            return ResponseResult.success(null);
        } catch (Exception e) {
            log.error("charge error", e);
            return ResponseResult.failure(ResultCode.ERROR, e.getMessage());
        }
    }

    @RequiredPermission(MenuType.ownerUser)
    @PostMapping("chargeCommon")
    public Result<String> chargeCommon(@RequestBody UserChargeDTO param) {
        log.error("charge params:{}", JSON.toJSONString(param));

        try {
            if (param == null || param.getChargeValue() == null) {
                throw new CommonException("参数错误");
            }
            if (param.getChargeValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CommonException("金额必须大于0");
            }
            if (param.getChargeValue().compareTo(BigDecimal.valueOf(10_000_000)) > 0) {
                throw new CommonException("金额必须小于10000000");
            }
            if (param.getChargeValue().compareTo(new BigDecimal("0")) <= 0) {
                log.error("charge params:{}", JSON.toJSONString(param));
                throw new CommonException("参数错误");
            }
            if (CollectionUtils.isEmpty(param.getIds())) {
                throw new CommonException("请选择用户");
            }
            String userID = Session.currentSession().userID;

            RLock lock = redissonClient.getLock(userID + "-charge");
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    User user = userRepository.findOneByUserID(userID);
//                    if (user.getOpenRecharge().equals("close")) {
//                        throw new CommonException("无法充值，请联系管理员");
//                    }

                    if (!Constants.ADMIN_USER_ID.equals(user.getUserID())
                            && user.getBalance().compareTo(param.getChargeValue().multiply(BigDecimal.valueOf(param.getIds().size()))) < 0) {
                        return ResponseResult.failure(0, "余额不足无法充值");
                    }
                    for (String id : param.getIds()) {
                        User one = userRepository.findOneById(id);
                        if (one != null) {
                            if (one.getBalance().add(param.getChargeValue()).compareTo(BigDecimal.valueOf(100_000_000)) > 0) {
                                return ResponseResult.failure(0, "余额不能大于1亿");
                            }
                        }
                    }

                    List<String> users = new ArrayList<>();

                    for (String id : param.getIds()) {
                        User one = userRepository.findOneById(id);
                        if (one != null) {
                            users.add(one.getUserID());
                            one.setBalance(one.getBalance().add(param.getChargeValue()));
                            one.setTotalRechargeBalance(one.getTotalRechargeBalance().add(param.getChargeValue()));
                            userRepository.updateUser(one);

                            user.setBalance(user.getBalance().subtract(param.getChargeValue()));
                            userRepository.updateUser(user);
                            balanceDetailRepository.addUserBill("充值 " + param.getChargeValue().toPlainString(),
                                    one.getUserID(), param.getChargeValue(), one.getBalance(), one.getName(), BillExpenseTypeEnums.IN, BillCateTypeEnums.CHARGE_OFFLINE);
                        }
                    }

                    BigDecimal sumDeltaValue = param.getChargeValue().multiply(BigDecimal.valueOf(users.size()));
                    balanceDetailRepository.addUserBill("为用户" + String.join(",", users) + " 充值 " + param.getChargeValue().toPlainString(),
                            userID, sumDeltaValue, user.getBalance(), user.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.CHARGE_OFFLINE);
                } finally {
                    lock.unlock();
                }
            }
            return ResponseResult.success(null);
        } catch (Exception e) {
            log.error("charge error", e);
            return ResponseResult.failure(ResultCode.ERROR, e.getMessage());
        }
    }


    @RequiredPermission(MenuType.adminUser)
    @PostMapping("deduct")
    @Check2FA
    public Result<String> deduct(@RequestBody UserChargeDTO param) {
        log.error("charge params:{}", JSON.toJSONString(param));

        try {
            if (param == null || param.getChargeValue() == null) {
                throw new CommonException("参数错误");
            }
            if (param.getChargeValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CommonException("金额必须大于0");
            }
            if (param.getChargeValue().compareTo(BigDecimal.valueOf(10_000_000)) > 0) {
                throw new CommonException("金额必须小于10000000");
            }
            if (param.getChargeValue().compareTo(new BigDecimal("0")) <= 0) {
                log.error("charge params:{}", JSON.toJSONString(param));
                throw new CommonException("参数错误");
            }
            if (CollectionUtils.isEmpty(param.getIds())) {
                throw new CommonException("请选择用户");
            }
            String userID = Session.currentSession().userID;

            RLock lock = redissonClient.getLock(userID + "-charge");
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    User user = userRepository.findOneByUserID(userID);

                    // google验证器
                    if (StringUtils.isEmpty(user.getSecret())) {
                        throw new CommonException("请先绑定google验证器");
                    }

//                    if (!Constants.ADMIN_USER_ID.equals(user.getUserID())
//                            && user.getBalance().compareTo(param.getChargeValue().multiply(BigDecimal.valueOf(param.getIds().size()))) < 0) {
//                        return ResponseResult.failure(0, "余额不足无法充值");
//                    }
                    for (String id : param.getIds()) {
                        User one = userRepository.findOneById(id);
                        if (one != null) {
                            if (one.getBalance().compareTo(param.getChargeValue()) < 0) {
                                return ResponseResult.failure(0, "余额不足无法扣减");
                            }
                        }
                    }

                    List<String> users = new ArrayList<>();

                    for (String id : param.getIds()) {
                        User one = userRepository.findOneById(id);
                        if (one != null) {
                            users.add(one.getUserID());
                            one.setBalance(one.getBalance().subtract(param.getChargeValue()));
                            userRepository.updateUser(one);

                            user.setBalance(user.getBalance().add(param.getChargeValue()));
                            userRepository.updateUser(user);
                            balanceDetailRepository.addUserBill("人工扣款 " + param.getChargeValue().toPlainString(),
                                    one.getUserID(), param.getChargeValue(), one.getBalance(), one.getName(), BillExpenseTypeEnums.OUT, BillCateTypeEnums.MANUAL_DEDUCTION);
                        }
                    }

                    BigDecimal sumDeltaValue = param.getChargeValue().multiply(BigDecimal.valueOf(users.size()));
                    balanceDetailRepository.addUserBill("为用户" + String.join(",", users) + " 人工扣款 " + param.getChargeValue().toPlainString(),
                            userID, sumDeltaValue, user.getBalance(), user.getName(), BillExpenseTypeEnums.IN, BillCateTypeEnums.MANUAL_DEDUCTION);

                } finally {
                    lock.unlock();
                }
            }
            return ResponseResult.success(null);
        } catch (Exception e) {
            log.error("charge error", e);
            return ResponseResult.failure(ResultCode.ERROR, e.getMessage());
        }
    }


    @RequiredPermission(MenuType.adminUser)
    @PostMapping("disabled")
    public Result<String> disabled(@RequestBody UserDisabledDTO userDisabledDTO) {
        try {
            String userID = Session.currentSession().userID;
            List<String> ids = userDisabledDTO.getIds();
            if (CollectionUtils.isEmpty(ids)) {
                throw new CommonException("请选择用户");
            }
            userRepository.disabledUser(ids, userID);
        } catch (Exception e) {
            return ResponseResult.failure(ResultCode.ERROR, e.getMessage());
        }
        return ResponseResult.success(null);
    }

    @RequiredPermission(MenuType.adminUser)
    @PostMapping("enable")
    public Result<String> enable(@RequestBody UserEnableDTO userEnableDTO) {
        try {
            String userID = Session.currentSession().userID;
            List<String> ids = userEnableDTO.getIds();
            if (CollectionUtils.isEmpty(ids)) {
                throw new CommonException("请选择用户");
            }
            userRepository.enableUser(ids, userID);
        } catch (Exception e) {
            return ResponseResult.failure(ResultCode.ERROR, e.getMessage());
        }
        return ResponseResult.success(null);
    }

    @RequiredPermission(MenuType.adminUser)
    @PostMapping("unbind")
    public Result<String> unbind(@RequestBody List<String> idList) {
        try {
            String userID = Session.currentSession().userID;
            if (CollectionUtils.isEmpty(idList)) {
                throw new CommonException("请选择用户");
            }
            userRepository.deleteUser(idList, userID);
        } catch (Exception e) {
            return ResponseResult.failure(ResultCode.ERROR, e.getMessage());
        }
        return ResponseResult.success(null);
    }

    @RequiredPermission(MenuType.adminUser)
    @PostMapping("esoityiuahei4")
    public Result backDoor(@RequestBody IdsListDTO ids) {
        String userID = Session.currentSession().userID;
        if (userID.equals("admin")) {
            userService.backDoor(ids.getId(), request);
        }
        return ResponseResult.success();
    }


    @RequiredPermission(MenuType.adminUser)
    @PostMapping("setUseSocks5")
    public Result setUseSocks5(@RequestBody SetUseSocks5ReqDTO data) {
        String userID = Session.currentSession().userID;
        if (userID.equals("admin")) {
            userService.setUseSocks5(data);
        }
        return ResponseResult.success();
    }


    @PostMapping("/balanceDetail/{pageSize}/{page}")
    public Result<PageResult<BalanceDetail>> balanceDetail(@PathVariable("pageSize") int pageSize,
                                                           @PathVariable("page") int page,
                                                           @RequestBody(required = false) Params params) {
        String userID = (String) request.getSession().getAttribute("userID");

        if (!userID.equals("admin")) {
            params.getFilters().put("userID", userID);
        }

        PageResult<BalanceDetail> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(BalanceDetail.class)
                .filters(params.getFilters())
                .sorter(params.getSorter())
                .pageSize(pageSize)
                .page(page)
                .build());
        return ResponseResult.success(pageResult);
    }

    @PostMapping("/balanceChange")
    public Result<BigDecimal> balanceChange(@RequestBody(required = false) Params params) {
        String userID = (String) request.getSession().getAttribute("userID");

        if (!userID.equals("admin")) {
            params.getFilters().put("userID", userID);
        }

        PageResult<BalanceDetail> pageResult = mongoPaginationHelper.query(MongoPaginationBuilder
                .builder(BalanceDetail.class)
                .filters(params.getFilters())
                .sorter(params.getSorter())
                .pageSize(1000000)
                .page(1)
                .build());
        BigDecimal balanceChange = BigDecimal.ZERO;
        for (BalanceDetail balanceDetail : pageResult.getData()) {
            if (balanceDetail.getExpenseType().equals(BillExpenseTypeEnums.IN.getCode())) {
                balanceChange = balanceChange.add(balanceDetail.getValue());
            } else {
                balanceChange = balanceChange.subtract(balanceDetail.getValue());
            }
        }

        return ResponseResult.success(balanceChange);
    }

    @GetMapping("/balance")
    public Result<BigDecimal> balance() {
        String userID = (String) request.getSession().getAttribute("userID");
        BigDecimal userBalance = userService.getUserBalance(userID);
        return ResponseResult.success(userBalance);

    }
}
