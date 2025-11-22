package com.nyy.gmail.cloud.controller.common;

import cn.hutool.core.util.StrUtil;

import com.alibaba.fastjson2.JSON;
import com.nyy.gmail.cloud.model.dto.CommonUserDTO;
import com.nyy.gmail.cloud.model.dto.CommonUserEditDTO;
import com.nyy.gmail.cloud.model.dto.GraphCodeDTO;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.utils.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.common.enums.SourceEnum;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Menu;
import com.nyy.gmail.cloud.entity.mongo.Role;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.model.vo.CommonUserEditVO;
import com.nyy.gmail.cloud.model.vo.CommonUserPermissionsVO;
import com.nyy.gmail.cloud.model.vo.CommonUserVO;
import com.nyy.gmail.cloud.model.vo.GraphCodeVO;
import com.nyy.gmail.cloud.repository.mongo.MenuRepository;
import com.nyy.gmail.cloud.repository.mongo.RoleRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.service.IpStatisticsService;
import com.nyy.gmail.cloud.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.*;


@Slf4j
@Component
@RestController
@RequestMapping("/api/common/user/")
public class CommonUserController {
    @Value("${login.onlyOneLoginUser}")
    private Boolean onlyOneLoginUser;

    @Value("${login.adminBackNotOnly}")
    private Boolean adminBackNotOnly;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IpStatisticsService ipStatisticsService;

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    MongoTemplate mongoTemplate;

    @Resource
    private RedisUtil redisUtil;

    @NoLogin
    @PostMapping("login")
    public Result<CommonUserVO> login(@RequestBody CommonUserDTO commonUserDTO) {
        String graphCodeKey = commonUserDTO.getGraphCodeKey();
        String code = commonUserDTO.getCode();
        if (StringUtils.isBlank(commonUserDTO.getUserID()) && StringUtils.isBlank(commonUserDTO.getPassword())) {
            return ResponseResult.failure(ResultCode.ERROR, "请输入用户名、密码");
        }
        if (StringUtils.isBlank(commonUserDTO.getUserID())) {
            return ResponseResult.failure(ResultCode.ERROR, "请输入用户名");
        }
        if (StringUtils.isBlank(commonUserDTO.getPassword())) {
            return ResponseResult.failure(ResultCode.ERROR, "请输入密码");
        }

//        if (StringUtils.isAnyBlank(code, graphCodeKey) || code.length() != 5 || graphCodeKey.length() != 32 + 4 ) {
//            return ResponseResult.failure(ResultCode.ERROR, "验证码不能为空或者验证码长度不为5");
//        }
//
//        // 校验验证码
//        try {
//            userService.validateCode(code, graphCodeKey);
//        } catch (CommonException commonException){
//            return ResponseResult.failure(commonException.getCode(), commonException.getMessage());
//        }

        try {
            userService.checkLoginLock(commonUserDTO.getUserID());
        } catch (CommonException commonException) {
            return ResponseResult.failure(commonException.getCode(), commonException.getMessage());
        }

        try {
            // 生成一个MD5加密计算摘要
            String pwd = MD5Util.MD5(MD5Util.MD5(commonUserDTO.getUserID()) + commonUserDTO.getPassword());
            commonUserDTO.setPassword(pwd);
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
//                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            String sessionId = session.getId();
            User user = userRepository.findOneByPassword(commonUserDTO.getUserID(), commonUserDTO.getPassword());
            if (user != null) {
                if (!"enable".equals(user.getStatus())) {
                    return ResponseResult.failure(ResultCode.ERROR, "用户该账号已被冻结，请联系管理员");
                }
//                if (StringUtils.isNotEmpty(user.getSecret())) {
//                    CommonUserVO commonUserVO = new CommonUserVO();
//                    commonUserVO.setBiz(UUID.randomUUID().toString());
//                    redisUtil.setUser2FA(user.getUserID(), commonUserVO.getBiz());
//                    return ResponseResult.failure(-1, "2fa", commonUserVO);
//                }
                // long group = accountGroupRepository.countByUserID(user.getUserID());
                // long label = friendLabelRepository.countByUserID(user.getUserID());
                session.setAttribute("userID", user.getUserID());
                session.setAttribute("LoginUserID", user.getUserID());
                session.setAttribute("session", sessionId);//user.getSession()是上一个
                session.setAttribute("source", SourceEnum.WEB.name());

//                request.getSession().setAttribute("role", user.getRole());//这里的role在上方方法中改成了对应角色的名字
                CommonUserVO commonUserVO = new CommonUserVO();
                commonUserVO.setUserID(user.getUserID());
                commonUserVO.setName(user.getName());
                // commonUserVO.setGroup(group);
                // commonUserVO.setLabel(label);
                commonUserVO.setCustomer(user.getCustomer());
                commonUserVO.setBalance(user.getBalance());
                commonUserVO.setFrozenBalance(user.getFrozenBalance());
                commonUserVO.setSetSecretKey(StringUtils.isNotEmpty(user.getSecret()));
                commonUserVO.setUserApiKey(user.getUserApiKey());
                commonUserVO.setOpenRecharge(user.getOpenRecharge());
                commonUserVO.setRestSendEmailCount(user.getRestSendEmailCount());
                // commonUserVO.setServerVersion(userService.getVersion());
                Result<CommonUserVO> result = ResponseResult.success(commonUserVO);

                // 进行ip统计
                String ip = IpUtil.getIpAddr(request);
                ipStatisticsService.record(user.getUserID(), ip);
                userService.loginSuccess(commonUserDTO.getUserID());
                userRepository.findOneAndUpdateByPassword(commonUserDTO.getUserID(), commonUserDTO.getPassword(), sessionId);
                return result;
            }

            user = userRepository.addUserLoginFailedTime(commonUserDTO.getUserID());
            // userID对应的用户不一定存在
            if (user == null) {
                return ResponseResult.failure(ResultCode.LOGIN_ERROR.getCode(), "用户名或密码错误");
            }
            userService.loginFail(user, commonUserDTO.getUserID());
            if (user.getLoginFailedTime() >= 5) {
                return ResponseResult.failure(ResultCode.ERROR, "用户该账号已被冻结，请联系管理员");
            }
            return ResponseResult.failure(ResultCode.LOGIN_ERROR.getCode(), "用户名或密码错误");

        } catch (Exception e) {
            Result<CommonUserVO> result = ResponseResult.failure(ResultCode.ERROR, e.getMessage());
            return result;
        }
    }

    @NoLogin
    @GetMapping("getGraphCode")
    public Result<GraphCodeVO> getGraphCode() {
        GraphCodeDTO graphCode = userService.getGraphCode();
        GraphCodeVO graphCodeVO = new GraphCodeVO();
        BeanUtils.beanCopy(graphCode, graphCodeVO);
        return ResponseResult.success(graphCodeVO);
    }

    @NoLogin
    @GetMapping(value = "stat")
    public Result<CommonUserVO> state() {
        String LoginUserID = (String) request.getSession().getAttribute("LoginUserID");
        String userID = (String) request.getSession().getAttribute("userID");
        String sessionId = (String) request.getSession().getAttribute("session");

        if (LoginUserID != null && !LoginUserID.equals("")) {
            //同一个账号可以多次登录
            User user = userRepository.findOneByUserID(userID);
//            if (userID.equals("admin")) {
//                user = userRepository.findOneByUserID(userID);
//            } else {
//                user = userRepository.findOneAndUpdateBySession(userID, sessionId);
//            }
            if (user == null) {
//                request.getSession().invalidate();
            } else {
                // long group = accountGroupRepository.countByUserID(user.getUserID());
                // long label = friendLabelRepository.countByUserID(user.getUserID());

                Role role = roleRepository.findRoleById(user.getRole());
                CommonUserVO commonUserVO = new CommonUserVO();
                commonUserVO.setUserID(user.getUserID());
                commonUserVO.setName(user.getName());
                commonUserVO.setBalance(user.getBalance());
                commonUserVO.setFrozenBalance(user.getFrozenBalance());
                commonUserVO.setUserApiKey(user.getUserApiKey());
                commonUserVO.setOpenRecharge(user.getOpenRecharge());
                commonUserVO.setRestSendEmailCount(user.getRestSendEmailCount());
                commonUserVO.setRole(role.getName());
                // commonUserVO.setGroup(group);
                // commonUserVO.setLabel(label);
                commonUserVO.setCustomer(user.getCustomer());
                // 是否设置谷歌secret
                commonUserVO.setSetSecretKey(StringUtils.isNotEmpty(user.getSecret()));
                // commonUserVO.setServerVersion(userService.getVersion());
                Result<CommonUserVO> result = ResponseResult.success(commonUserVO);
                return result;
            }
        }

        Result<CommonUserVO> result = ResponseResult.failure(ResultCode.ERROR, null);
        return result;
    }

    @PostMapping(value = "logout")
    public Result<CommonUserVO> logout() {
        request.getSession().invalidate();

        Result<CommonUserVO> result = ResponseResult.success(null);
        return result;
    }

    @GetMapping(value = "loadPermissions")
    public Result<List<Menu>> loadPermissions() {
        String userID = (String) request.getSession().getAttribute("userID");
        if (userID == null || userID.equals("")) {
            Result<List<Menu>> result = ResponseResult.failure(ResultCode.ERROR, null);
            return result;
        }
        User user = userRepository.findOneByUserID(userID);
        Role role = null;
        List<Menu> permissions = null;
        if (user != null) role = roleRepository.findRoleById(user.getRole());
        if (role != null) permissions = menuRepository.findMenusByIds(role.getPermissions());

        if (user == null || role == null) {
            Result<List<Menu>> result = ResponseResult.failure(ResultCode.ERROR, null);
            return result;
        }

        CommonUserPermissionsVO commonUserPermissionsVO = new CommonUserPermissionsVO();
        commonUserPermissionsVO.setPermissions(permissions);
        Result<List<Menu>> result = ResponseResult.success(permissions);
        return result;
    }

    @PostMapping("edit")
    public Result<CommonUserEditVO> edit(@RequestBody CommonUserEditDTO commonUserEditDTO) {
        if (commonUserEditDTO.getUserID() != null && !commonUserEditDTO.getUserID().equals("") && !commonUserEditDTO.getUserID().equals("admin")) {
            User user = null;
            if (commonUserEditDTO.getPassword() != null && commonUserEditDTO.getNewPassword() != null && !commonUserEditDTO.getPassword().equals("") && !commonUserEditDTO.getNewPassword().equals("")) {
                try {
                    String pwd = MD5Util.MD5(MD5Util.MD5(commonUserEditDTO.getUserID()) + commonUserEditDTO.getPassword());
                    String newPwd = MD5Util.MD5(MD5Util.MD5(commonUserEditDTO.getUserID()) + commonUserEditDTO.getNewPassword());
                    String session = MD5Util.MD5(String.valueOf(new Date().getTime()));

                    user = userRepository.findOneByPassword(commonUserEditDTO.getUserID(), pwd);
                    if (user == null) {
                        Result<CommonUserEditVO> result = ResponseResult.failure(ResultCode.ERROR, null);
                        result.setMessage("原密码错误，请检查");
                        return result;
                    }
                    user = userRepository.findOneAndUpdateByNewPassword(commonUserEditDTO.getUserID(), pwd, session, newPwd);
                } catch (Exception e) {
                    Result<CommonUserEditVO> result = ResponseResult.failure(ResultCode.ERROR, e.getMessage());
                    return result;
                }
            } else if (commonUserEditDTO.getName() != null && commonUserEditDTO.getNewName() != null && !commonUserEditDTO.getName().equals("") && !commonUserEditDTO.getNewName().equals("")) {
                user = userRepository.findOneAndUpdateByNewName(commonUserEditDTO.getUserID(), commonUserEditDTO.getName(), commonUserEditDTO.getNewName());
            }
            if (user != null) {
                CommonUserEditVO commonUserEditVO = new CommonUserEditVO();
                commonUserEditVO.setUserID(commonUserEditDTO.getUserID());
                commonUserEditVO.setName(commonUserEditDTO.getNewName() != null && !commonUserEditDTO.getNewName().equals("") ? commonUserEditDTO.getNewName() : user.getName());
                Result<CommonUserEditVO> result = ResponseResult.success(commonUserEditVO);
                return result;
            }
        }

        Result<CommonUserEditVO> result = ResponseResult.failure(ResultCode.ERROR, null);
        return result;
    }

//    @PostMapping("switchUsers")
//    public Result<Void> switchUser(@RequestBody Params params) {
//        String userID = (String) request.getSession().getAttribute("userID");
//        if (!userID.isEmpty()) {
//            String session = null;
//            if (onlyOneLoginUser && !(adminBackNotOnly && "admin".equals(userID))) {
//                session = MD5Util.MD5(String.valueOf(new Date().getTime()));
//            }
//            User user = userRepository.updateLogin(userID, params.getSelectedRowKey(), new Date(), session);
//            if (user != null) {
//                request.getSession().setAttribute("userID", user.getUserID());
//                request.getSession().setAttribute("_userID", user.getUserID());
//                request.getSession().setAttribute("LoginUserID", user.getUserID());
//                request.getSession().setAttribute("session", user.getSession());
//                return ResponseResult.success();
//            }
//            return ResponseResult.failure(ResultCode.ERROR, "未检测到对应的用户信息！");
//        }
//        return ResponseResult.failure(ResultCode.ERROR, "未检测到登录信息！");
//    }

//    /**
//     * 删除用户,禁用
//     *
//     * @param users
//     * @return
//     */
//    @PostMapping("dropUser")
//    public Result<Void> dropUser(@RequestBody List<String> users) {
//        String userID = (String) request.getSession().getAttribute("userID");
//        if (StrUtil.isNotBlank(userID)) {
//            //    userRepository.dropUser(userID, users);
//            //    return ResponseResult.success();
//            return ResponseResult.failure(ResultCode.ERROR, "不允许删除用户！");
//        }
//        return ResponseResult.failure(ResultCode.ERROR, "未检测到登录信息！");
//    }
}
