package com.nyy.gmail.cloud.common.aop;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;


@Slf4j
@Aspect
@Order(1)
@Component
public class ApiExceptionAop {

    private static final String START_TIME = "_START_TIME";

    @Pointcut("execution(public * com.nyy.gmail.cloud.common.exception.GlobalExceptionHandler.*(..))")
    public void apiMethodPointcut() {
    }


    @AfterReturning(pointcut = "apiMethodPointcut()",returning = "result")
    public void doAfterReturning(Object result) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();
        long durationTime = -1;
        Object attribute = request.getAttribute(START_TIME);
        if (attribute != null) {
            durationTime = System.currentTimeMillis() - Long.parseLong(attribute.toString());
        }

        String resultString = null;

        try{
            resultString = JSON.toJSONString(result);
        } catch (Exception e){
            resultString = result.toString();
            e.printStackTrace();
        }

        log.info("  END - {} - {} - {}ms - {}", request.getMethod(), request.getRequestURI(), durationTime, resultString);
    }
}
