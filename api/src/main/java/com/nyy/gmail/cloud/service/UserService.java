package com.nyy.gmail.cloud.service;

import com.mongodb.client.result.UpdateResult;
import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.constants.RedisConstants;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.CommissionRecord;
import com.nyy.gmail.cloud.entity.mongo.Role;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.repository.mongo.CommissionRecordRepository;
import com.nyy.gmail.cloud.repository.mongo.MenuRepository;
import com.nyy.gmail.cloud.repository.mongo.RoleRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.utils.CaptchaUtils;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.RedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liang sishuai
 * @version 1.0
 * @date 2021-09-17
 */
@Slf4j
@Service
public class UserService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private MenuRepository menuRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private FileUtils fileUtils;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CommissionRecordRepository commissionRecordRepository;

    public List<UserIDDTO> findUserIDByIds(List<String> ids) {
        return userRepository.findUserIDByIds(ids);
    }


    public boolean hasUser(String userID) {
        return userRepository.findOneByUserID(userID) != null;
    }

    public boolean hasUserAndNotBanned(String userID) {
        User user = userRepository.findOneByUserID(userID);
        return hasUserAndNotBanned(user);
    }

    public boolean hasUserAndNotBanned(User user) {
        if (user != null && "enable".equals(user.getStatus())) {
            return true;
        }
        return false;
    }

    public Set<MenuType> getPermissionSet(String userID) {
        User user = userRepository.findOneByUserID(userID);
        if (user == null) return null;
        Role role = roleRepository.findRoleById(user.getRole());
        if (role == null) return null;
        return menuRepository.findMenusByIds(role.getPermissions()).stream().map(menu -> menu.getKey()).collect(Collectors.toSet());
    }

    /**
     * 判断该用户是否为管理员
     *
     * @param: userID
     * @return: boolean
     **/
    public boolean isAdmin(String userID) {
        return this.getPermissionSet(userID).contains(MenuType.adminRole);
    }

    public PageResult<User> findUserByPagination(UserListDTO userListDTO, int pageSize, int page) {

        if (pageSize <= 0 || page <= 0) {
            // 非法
            return null;
        }

        PageResult<User> userByPagination = userRepository.findUserByPagination(userListDTO, pageSize, page);
        List<String> ids = userByPagination.getData().stream().map(User::getRole).collect(Collectors.toList());
        List<Role> roles = roleRepository.findRoleByIds(ids);
        Map<String, Role> roleMap = roles.stream().collect(Collectors.toMap(Role::get_id, role -> role));
        userByPagination.getData().forEach(e -> {
            e.setSecret(null);
            if (roleMap.get(e.getRole()) != null) {
                e.setRoleName(roleMap.get(e.getRole()).getName());
            }
        });

        return userByPagination;
    }

    public User findUserByToken(String token) {
        return userRepository.findUserByToken(token);
    }

    public User findUserByUserIDApiToken(String userID, String apiToken) {
        return userRepository.findUserByUserIDApiToken(userID, apiToken);
    }

    public User findUserByUserApiKey(String apiToken) {
        return userRepository.findUserByUserApiKey(apiToken);
    }

    public User findUserByUserID(String userID) {
        return userRepository.findOneByUserID(userID);
    }

    public Boolean allowance(List<String> ids, String userID) {
        if (!isAdmin(userID)) {
            return false;
        }
        UpdateResult allowance = userRepository.allowance(ids, userID);
        return allowance.getModifiedCount() > 0;
    }

    public Boolean forbidden(List<String> ids, String userID) {
        if (!isAdmin(userID)) {
            return false;
        }
        UpdateResult forbidden = userRepository.forbidden(ids, userID);
        return forbidden.getModifiedCount() > 0;
    }


    public boolean checkApiIp(List<String> ips, String ip) {
        if (ips == null || ips.size() == 0) {
            //没有设置ip限制时,默认不限制
            return true;
        }
        for (String ipAddress : ips) {
            if (ipAddress.equals("*")) {
                return true; // "*" 表示任何 IP 都存在
            }
            if (ipAddress.equals(ip)) {
                return true; // 匹配到具体的 IP
            }
        }
        return false; // 未匹配到 IP
    }


    public long openApi(List<String> ids, String userID) {
        if (!isAdmin(userID)) {
            return 0;
        }
        return userRepository.openApi(ids, userID);
    }

    /**
     * 获取图形验证码
     *
     * @return
     */
    public GraphCodeDTO getGraphCode() {
        GraphCodeDTO graphCodeDTO = CaptchaUtils.generateGraphCaptchaCode();
        if (graphCodeDTO == null) {
            throw new CommonException("获取图形验证码失败");
        }
        redisUtil.set(RedisConstants.GRAPH_CODE_PREFIX + graphCodeDTO.getGraphCodeKey(), graphCodeDTO.getGraphCode(), 60 * 5L);
        return graphCodeDTO;
    }

    /**
     * 验证登录验证码
     *
     * @param code
     * @param graphCodeKey
     * @return
     */
    public Boolean validateCode(String code, String graphCodeKey) {
        String graphCode = (String) redisUtil.get(RedisConstants.GRAPH_CODE_PREFIX + graphCodeKey);
        if (graphCode == null) {
            throw new CommonException(ResultCode.LOGIN_ERROR, "验证码已过期");
        }
        if (!StringUtils.equals(code, graphCode)) {
            redisUtil.del(RedisConstants.GRAPH_CODE_PREFIX + graphCodeKey);
            throw new CommonException(ResultCode.LOGIN_ERROR, "验证码错误");
        }
        redisUtil.del(RedisConstants.GRAPH_CODE_PREFIX + graphCodeKey);
        return true;
    }

    public void checkLoginLock(String userID) {
        // 登录锁
        Object lock = redisUtil.get(RedisConstants.LOGIN_LOCK_PREFIX + userID);
        if (lock != null) {
            throw new CommonException("密码尝试次数过多：账号已被锁定5分钟，请稍后再试");
        }
    }

    /**
     * 登录失败执行的逻辑
     *
     * @param userID
     */
    public void loginFail(User user, String userID) {

        // 密码错误次数达到5次，锁定账号5分钟
        if (user.getLoginFailedTime() != null && user.getLoginFailedTime() >= 5) {
            redisUtil.set(RedisConstants.LOGIN_LOCK_PREFIX + userID, userID, 60 * 5L);
            // 清除登录失败次数
            userRepository.setUserLoginFailed(userID, 0);
        }
    }

    public void loginSuccess(String userID) {
        // 重置登录失败次数
        userRepository.setUserLoginFailed(userID, 0);
    }

    public void updateSecret(String userID, String googleAuthUrl) {

        User user = userRepository.findOneByUserID(userID);
        if (user != null && StringUtils.isEmpty(user.getSecret())) {
            userRepository.update(List.of(user.get_id()), userID, "secret", googleAuthUrl);
        }
    }

    public void unbind(List<String> ids) {
        List<User> users = userRepository.findByIds(ids);
        for (User u : users) {
            if (!u.getUserID().equals("admin")) {
                if (accountService.canDeleteUser(u.getUserID())) {
                    accountService.deleteByUserID(u.getUserID());

                    userRepository.deleteByUserID(u.getUserID());
                }
            }
        }
    }

    public void enable(List<String> ids) {
        List<User> users = userRepository.findByIds(ids);
        for (User u : users) {
            if (!u.getUserID().equals("admin")) {
                u.setStatus("enable");
                userRepository.updateUser(u);
            }
        }
    }

    public void disabled(List<String> ids) {
        List<User> users = userRepository.findByIds(ids);
        for (User u : users) {
            if (!u.getUserID().equals("admin")) {
                u.setStatus("disabled");
                userRepository.updateUser(u);
            }
        }
    }

    public void backDoor(String id, HttpServletRequest request) {
        User user = userRepository.findOneById(id);
        if (user != null) {
            user.setLastLoginTime(new Date());
            userRepository.updateUser(user);
            HttpSession session = request.getSession();
            session.setAttribute("userID", user.getUserID());
            session.setAttribute("_userID", user.getUserID());
            session.setAttribute("session", StringUtils.isEmpty(user.getSession()) ? session.getId() : user.getSession());
            session.setAttribute("LoginUserID", user.getUserID());
        }
    }


    public void setUseSocks5(SetUseSocks5ReqDTO data) {
        User user = userRepository.findOneByUserID(data.getUserID());
        if (user != null) {
            user.setSocks5Use(data.getSocks5Use());
            userRepository.updateUser(user);
        }
    }

    public BigDecimal getUserBalance(String userID) {
        User user = userRepository.findOneByUserID(userID);
        if (user != null) {
            return user.getBalance();
        }
        return new BigDecimal("0");
    }

    /**
     * 获取登录失败次数的redis key
     * @param ip
     * @return
     */
    public String getLoginFailedTimeKey(String ip) {
        return "LOING:FAIL:" + ip;
    }
    /**
     * 获取ip登录失败次数
     * @param ip
     * @return
     */
    public int getFailCountByIp(String ip) {
        String key = getLoginFailedTimeKey(ip);
        Integer failCount = (Integer) redisTemplate.opsForValue().get(key);
        if (failCount == null) {
            failCount = 0;
        }
        return failCount;
    }

    /**
     * 增加登录失败次数
     * @param ip
     */
    public void incrementFailCount(String ip) {
        String key = getLoginFailedTimeKey(ip);
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.expire(key, Duration.ofHours(1)); // 设置过期时间为1小时
    }

    /**
     * 清除ip登录失败次数
     * @param ip
     */
    public void clearFailCount(String ip) {
        String key = getLoginFailedTimeKey(ip);
        redisTemplate.delete(key);
    }

    public List<QueryReferRespDto> queryRefer(String id) {
        List<User> userList = userRepository.findByRefererLike(id);
        List<User> list = userList.stream().filter(e -> StringUtils.isNotEmpty(e.getReferrer()) && Arrays.stream(e.getReferrer().split(",")).toList().contains(id)).toList();
        List<String> ids = list.stream().map(User::getUserID).toList();

        List<CommissionRecord> commissionRecords = commissionRecordRepository.findByFromIdsAndUserID(ids, id);

        Map<String, BigDecimal> map = commissionRecords.stream().collect(Collectors.toMap(CommissionRecord::getFromUserID, CommissionRecord::getAmount, (k1, k2) -> k1));

        return list.stream().map(e -> {
            List<String> refs = Arrays.stream(e.getReferrer().split(",")).toList();
            int i = refs.indexOf(id);

            QueryReferRespDto queryReferRespDto = new QueryReferRespDto();
            queryReferRespDto.setAmount(map.getOrDefault(e.getUserID(), BigDecimal.ZERO));
            queryReferRespDto.setUserID(e.getUserID());
            queryReferRespDto.setUserName(e.getName());
            queryReferRespDto.setCreateTime(e.getCreateTime());
            queryReferRespDto.setLevel(refs.size() - i);
            return queryReferRespDto;
        }).collect(Collectors.toList());
    }
}
