package com.nyy.gmail.cloud.common.aop;

import com.nyy.gmail.cloud.common.annotation.NoLogin;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.exception.NoLoginException;
import com.nyy.gmail.cloud.common.exception.PermissionException;
import com.nyy.gmail.cloud.service.UserService;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Order(6)
@Component
public class CheckPermissionAop {

    @Autowired
    private UserService userService;

    @Pointcut("execution(public * com.nyy.gmail.cloud.controller.*.*(..))")
    public void checkPermission() {
    }

    @Before("checkPermission()")
    public void outInfo(JoinPoint pjp) throws PermissionException {
        Signature signature = pjp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method targetMethod = methodSignature.getMethod();
        if (targetMethod.isAnnotationPresent(NoLogin.class)) {
            return;
        }
        Class<?> clazz = pjp.getTarget().getClass();
        Set<MenuType> requiredPermissions = new HashSet<>();
        if (clazz.isAnnotationPresent(RequiredPermission.class)) {      //  如果类上有注解
            //获取类上注解中表明的权限
            RequiredPermission requiredPermission = (RequiredPermission) clazz.getAnnotation(RequiredPermission.class);
            requiredPermissions.addAll(Arrays.asList(requiredPermission.value()));
        }
        if (targetMethod.isAnnotationPresent(RequiredPermission.class)) {        //  如果方法上有注解
            //获取方法上注解中表明的权限
            RequiredPermission requiredPermission = targetMethod.getAnnotation(RequiredPermission.class);
            requiredPermissions.addAll(Arrays.asList(requiredPermission.value()));
        }

        if (requiredPermissions.size() == 0) return;        //  如果没有设置权限代表全都可以访问

        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new NoLoginException("无法获取请求上下文");
        }
        HttpServletRequest request = attributes.getRequest();
        String userID = Session.currentSession().userID;
        // redis或数据库 中获取该用户的权限信息 并判断是否有权限
        Set<MenuType> permissionSet = userService.getPermissionSet(userID);
        if (!CollectionUtils.isEmpty(permissionSet)) {
            for (MenuType requiredPermission : requiredPermissions) {
                if (permissionSet.contains(requiredPermission)) return;     //  用户只要有一个权限就可以访问
            }
        }
        log.info("用户:{} 无法访问api:{}...", userID, request.getRequestURI());
        //如果没有权限,抛出异常,由Spring框架捕获,跳转到错误页面
        throw new PermissionException();
    }
}
