package com.nyy.gmail.cloud.common.aop;

import lombok.extern.slf4j.Slf4j;
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

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.common.enums.SourceEnum;
import com.nyy.gmail.cloud.common.exception.NoLoginException;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.utils.IpUtil;

import cn.hutool.core.util.StrUtil;

import com.nyy.gmail.cloud.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Order(5)
@Component
public class CheckLoginAop {

    @Autowired
    private UserService userService;

    @Pointcut("execution(public * com.nyy.gmail.cloud.controller..*.*(..))")
    public void checkLogin() {
    }

    @Before("checkLogin()")
    public void outInfo(JoinPoint pjp) throws NoLoginException {

        Signature signature = pjp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method targetMethod = methodSignature.getMethod();
        Class<?> clazz = pjp.getTarget().getClass();
        // 判断是否有注解
        if (targetMethod.isAnnotationPresent(NoLogin.class)
                || clazz.isAnnotationPresent(NoLogin.class)) {        //  如果方法上有注解
            return;
        }

        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new NoLoginException("无法获取请求上下文");
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = IpUtil.getIpAddr(request);

        Session session = Session.currentSession();

        log.info("请求用户:{}", session);
        if (!session.isLogin()) {
//            request.getSession().invalidate();
            throw new NoLoginException("用户登录失效，请重新登录");
        }

        User user = userService.findUserByUserID(session.userID);
        if (!userService.hasUserAndNotBanned(user)) {
            throw new NoLoginException("用户不存在或已被禁用");
        }
        if (!"admin".equals(session.userID)) {
            //非api请求,判断session是否被更新
            if (SourceEnum.WEB.name().equals(session.source)) {
//                if (!session.session.equals(user.getSession())){
//                   request.getSession().invalidate();
//                    throw new NoLoginException("账号已经被登出");
//                }
            } else if (SourceEnum.API.name().equals(session.source)) {
                //api 验证ip
                if (!userService.checkApiIp(user.getIps(), ip)) {
                    throw new NoLoginException(StrUtil.format("该IP: {} 无权使用api", ip));
                }
            } else if (SourceEnum.CHATROOM.name().equals(session.source)) {
            }
        }
    }
}
