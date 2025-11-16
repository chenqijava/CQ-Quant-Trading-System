package com.nyy.gmail.cloud.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.common.enums.SourceEnum;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.model.vo.CommonUserVO;
import com.nyy.gmail.cloud.service.IpStatisticsService;
import com.nyy.gmail.cloud.service.UserService;
import com.nyy.gmail.cloud.utils.GoogleAuthenticatorUtils;
import com.nyy.gmail.cloud.utils.IpUtil;
import com.nyy.gmail.cloud.utils.RedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/user"})
public class TwoFAController {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private UserService userService;

    @Autowired
    private IpStatisticsService ipStatisticsService;

    @PostMapping("/getAuthenticationUrl")
    public Result<String> getAuthenticationUrl () {
        String userID = Session.currentSession().getUserID();
        String googleAuthUrl = redisUtil.getGoogleAuthUrl(userID);
        if (StringUtils.isNotEmpty(googleAuthUrl)) {
            return ResponseResult.success(GoogleAuthenticatorUtils.convertAuthUrl(userID, googleAuthUrl));
        } else {
            Map<String, String> authUrl = GoogleAuthenticatorUtils.getGoogleAuthUrl();
            redisUtil.setGoogleAuthUrl(userID, authUrl.get("secret"));
            return ResponseResult.success(authUrl.get("url"));
        }
    }

    @PostMapping("/setSecretKey")
    public Result setSecretKey () {
        String userID = Session.currentSession().getUserID();
        String googleAuthUrl = redisUtil.getGoogleAuthUrl(userID);
        if (StringUtils.isNotEmpty(googleAuthUrl)) {
            userService.updateSecret(userID, googleAuthUrl);
        }
        return ResponseResult.success();
    }

    @NoLogin
    @PostMapping("/checkAuthenticationCode")
    public Result<CommonUserVO> checkAuthenticationCode (@RequestBody Map<String, String> params, HttpServletRequest request) {
        String biz = params.getOrDefault("biz", "");
        String code = params.getOrDefault("code", "");
        String userID = redisUtil.getUser2fa(biz);
        if (StringUtils.isEmpty(userID)) {
            return ResponseResult.failure(ResultCode.LOGIN_ERROR.getCode(), "验证码不正确");
        }
        String sessionId = request.getSession().getId();

        User user = userService.findUserByUserID(userID);
        if (user != null) {
            boolean b = GoogleAuthenticatorUtils.verifyCode(code, user.getSecret());
            if (!b) {
                return ResponseResult.failure(ResultCode.LOGIN_ERROR.getCode(), "验证码不正确");
            }

            // long group = accountGroupRepository.countByUserID(user.getUserID());
            // long label = friendLabelRepository.countByUserID(user.getUserID());
            request.getSession().setAttribute("userID", user.getUserID());
            request.getSession().setAttribute("LoginUserID", user.getUserID());
            request.getSession().setAttribute("session", sessionId);//user.getSession()是上一个
            request.getSession().setAttribute("source", SourceEnum.WEB.name());

//                request.getSession().setAttribute("role", user.getRole());//这里的role在上方方法中改成了对应角色的名字
            CommonUserVO commonUserVO = new CommonUserVO();
            commonUserVO.setUserID(user.getUserID());
            commonUserVO.setName(user.getName());
            // commonUserVO.setGroup(group);
            // commonUserVO.setLabel(label);
            commonUserVO.setCustomer(user.getCustomer());
            commonUserVO.setSetSecretKey(StringUtils.isNotEmpty(user.getSecret()));
            // commonUserVO.setServerVersion(userService.getVersion());
            Result<CommonUserVO> result = ResponseResult.success(commonUserVO);

            // 进行ip统计
            String ip = IpUtil.getIpAddr(request);
            ipStatisticsService.record(user.getUserID(),ip);
            userService.loginSuccess(user.getUserID());
            return result;
        }

        return ResponseResult.failure(ResultCode.LOGIN_ERROR.getCode(), "验证码不正确");
    }

    @NoLogin
    @GetMapping("/qr")
    public void qr (@RequestParam String id, HttpServletResponse response) throws IOException {
        int width = 300;
        int height = 300;

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(id, BarcodeFormat.QR_CODE, width, height);

            response.setContentType("image/png");
            OutputStream os = response.getOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", os);
            os.flush();
            os.close();
        } catch (WriterException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "二维码生成失败");
        }
    }
}
