package com.nyy.gmail.cloud.common.aop;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Aspect
@Order(10)
@Component
public class ServiceLogAop {

    @Pointcut("execution(public * com.nyy.gmail.cloud.service..*.*(..))")
    public void apiMethodPointcut() {
    }

    @Around("apiMethodPointcut()")
    public Object serviceLog(ProceedingJoinPoint joinPoint) throws Throwable {

        Signature signature = joinPoint.getSignature();

        Object[] args = joinPoint.getArgs();
        List<Object> arguments = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Path) {
                arguments.add(args[i].toString());
            } else if (args[i] instanceof MultipartFile) {
                MultipartFile arg = (MultipartFile) args[i];
                HashMap<String, String> fileInfo = new HashMap<>();
                fileInfo.put("originalFilename", arg.getOriginalFilename());
                fileInfo.put("fileSize", String.valueOf(arg.getSize()));
                fileInfo.put("contentType", arg.getContentType());
                arguments.add(fileInfo);
            } else {
                arguments.add(args[i]);
            }
        }

        long start = System.currentTimeMillis();

//        log.info("SERVICE - BEGIN - {} - {} - {}", signature.getDeclaringTypeName(), signature.getName(), JSON.toJSONString(arguments));

        Object result = joinPoint.proceed();

        String resultString = result == null ? null : result.toString();

        if (resultString != null && resultString.length() > 256) {
            resultString = resultString.substring(0, 256) + "...";
        }
        long end = System.currentTimeMillis();

        if (end - start > 5000) {//5s
            log.warn("SERVICE - SLOW - {} - {} - {}ms - {}", signature.getDeclaringTypeName(), signature.getName(), end - start, resultString);
        }
//        log.info("SERVICE - END - {} - {} - {}ms - {}", signature.getDeclaringTypeName(), signature.getName(), end - start, resultString);

        return result;
    }

}
