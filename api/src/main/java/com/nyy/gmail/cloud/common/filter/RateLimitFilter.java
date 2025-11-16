package com.nyy.gmail.cloud.common.filter;

import com.nyy.gmail.cloud.service.ParamsService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter implements Filter {

    @Value("${application.taskType}")
    private String taskType;

    @Autowired
    private ParamsService  paramsService;

    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        // 每秒最多50个请求
        Bandwidth limit = Bandwidth.classic(taskType.equals("googleai") ? 100 : 10, Refill.greedy(taskType.equals("googleai") ? 100 : 10, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newBucket2() {
        // 每秒最多50个请求
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newBucket3() {
        // 每秒最多50个请求
        Bandwidth limit = Bandwidth.classic(1000, Refill.greedy(1000, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        } else {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest http = (HttpServletRequest) request;
        String clientIp = getClientIp(http);
        Bucket bucket = bucketCache.computeIfAbsent(clientIp, k -> newBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.info(clientIp + " 并发限制过 10qps");
            String whiteIp = paramsService.getParamsInMem("account.whiteIp", null, null).toString();
            if (!StringUtils.isEmpty(whiteIp)) {
                if (Arrays.stream(whiteIp.split(",")).toList().contains(clientIp)) {
                    Bucket bucket2 = bucketCache.computeIfAbsent(clientIp + "_1", k -> newBucket2());
                    if (bucket2.tryConsume(1)) {
                        chain.doFilter(request, response);
                        return;
                    }
                    log.info(clientIp + " 并发限制过 100qps");
                    String whiteIp2 = paramsService.getParamsInMem("account.whiteIp2", null, null).toString();
                    if (Arrays.stream(whiteIp2.split(",")).toList().contains(clientIp)) {
                        Bucket bucket3 = bucketCache.computeIfAbsent(clientIp + "_2", k -> newBucket3());
                        if (bucket3.tryConsume(1)) {
                            chain.doFilter(request, response);
                            return;
                        }
                        log.info(clientIp + " 并发限制过 1000qps");
                    }
                }
            }
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setStatus(429);
            resp.getWriter().write("Too many requests, please slow down.");
        }
    }
}