package com.nyy.gmail.cloud.common.aop;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.Check2FA;
import com.nyy.gmail.cloud.common.exception.NoLoginException;
import com.nyy.gmail.cloud.common.exception.TwoFaErrorException;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.service.UserService;
import com.nyy.gmail.cloud.utils.GoogleAuthenticatorUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Order(5)
@Component
public class Check2FAAop {

    @Autowired
    private UserService userService;

    @Pointcut("execution(public * com.nyy.gmail.cloud.controller..*.*(..))")
    public void check2FA() {
    }

    @Before("check2FA()")
    public void outInfo(JoinPoint pjp) throws TwoFaErrorException {
        Signature signature = pjp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method targetMethod = methodSignature.getMethod();
        Class<?> clazz = pjp.getTarget().getClass();
        // 判断是否有注解
        if (targetMethod.isAnnotationPresent(Check2FA.class)
        || clazz.isAnnotationPresent(Check2FA.class) ) {        //  如果方法上有注解
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                throw new NoLoginException("无法获取请求上下文");
            }
            HttpServletRequest request = attributes.getRequest();
            // 开放接口不需要验证码
            if (request.getRequestURI().contains("/api/open/")) {
                return;
            }

            Session session = Session.currentSession();
            User user = userService.findUserByUserID(session.userID);

            if (user != null && StringUtils.isNotEmpty(user.getSecret())) {
                String code = request.getHeader("auth-code");
                if (StringUtils.isEmpty(code)) {
                    throw new TwoFaErrorException();
                }
                try {
                    Integer.valueOf(code);
                } catch (Exception e) {
                    throw new TwoFaErrorException();
                }
                boolean b = GoogleAuthenticatorUtils.verifyCode(code, user.getSecret());
                if (!b) {
                    throw new TwoFaErrorException();
                }
            }
        }
    }
}
