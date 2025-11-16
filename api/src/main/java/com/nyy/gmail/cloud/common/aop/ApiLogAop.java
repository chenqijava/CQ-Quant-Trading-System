package com.nyy.gmail.cloud.common.aop;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@Aspect
@Order(10)
@Component
public class ApiLogAop {
    private static final String START_TIME = "_START_TIME";

    @Pointcut("execution(public * com.nyy.gmail.cloud.controller..*.*(..))")
    public void apiMethodPointcut(){
    }

    @Before("apiMethodPointcut()")
    public void before(JoinPoint joinPoint) {

        StringBuilder stringBuilder = new StringBuilder();
        Signature signature = joinPoint.getSignature();
        stringBuilder.append("METHOD : ");
        stringBuilder.append(signature.getDeclaringTypeName());
        stringBuilder.append(".");
        stringBuilder.append(signature.getName());

        Object[] args = joinPoint.getArgs();
        List<Object> arguments = new ArrayList<>();

        for (int i = 0; i < args.length ; i++ ){
            if (args[i] instanceof ServletRequest || args[i] instanceof ServletResponse ){
                continue;
            }
            if (args[i] instanceof MultipartFile ){
                MultipartFile arg = (MultipartFile) args[i];
                HashMap<String,String> fileInfo = new HashMap<>();
                fileInfo.put("originalFilename",arg.getOriginalFilename());
                fileInfo.put("fileSize", String.valueOf(arg.getSize()));
                fileInfo.put("contentType", arg.getContentType());
                arguments.add(fileInfo);
                continue;
            }
            if (args[i] instanceof Path){
                arguments.add(args[i].toString());
                continue;
            }
            arguments.add(args[i]);
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();
        request.setAttribute(START_TIME, System.currentTimeMillis());

        log.info("BEGIN - {} - {} - {}", request.getMethod(), request.getRequestURI(), JSON.toJSONString(arguments));
    }

    @AfterReturning(value = "apiMethodPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();
        long startTime = (long) request.getAttribute(START_TIME);

        String resultString = "null";

        try {
            if (result != null) {
                resultString = JSON.toJSONString(result, JSONWriter.Feature.LargeObject);
                if (resultString.length() > 81920) {
                    resultString = resultString.substring(0, 81920) + "...";
                }
            }
        } catch (Exception e) {
            resultString = Objects.toString(result);
            e.printStackTrace();
        }

        log.info("  END - {} - {} - {}ms - {}", request.getMethod(), request.getRequestURI(), System.currentTimeMillis() - startTime, resultString);
    }

}
