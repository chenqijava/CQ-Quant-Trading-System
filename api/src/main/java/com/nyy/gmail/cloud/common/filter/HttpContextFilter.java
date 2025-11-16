package com.nyy.gmail.cloud.common.filter;

import cn.hutool.core.util.StrUtil;
import com.nyy.gmail.cloud.common.enums.SourceEnum;
import com.nyy.gmail.cloud.common.exception.NoLoginException;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.service.UserService;
import com.nyy.gmail.cloud.utils.IpUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.nyy.gmail.cloud.common.Session;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@Order(2)
@WebFilter(urlPatterns = "/*")
public class HttpContextFilter implements Filter {

    // 用于完整跟踪一个请求
    public static final String TRACE_ID = "traceId";
    // 用于完整跟踪一个用户
    public static final String UID = "userID";

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private UserService userService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            if (httpServletRequest.getMethod().equals(HttpMethod.OPTIONS.name())) {
                chain.doFilter(request, response);
                return;
            }

            // swagger直接跳过
            if (httpServletRequest.getRequestURI().startsWith("/swagger")
                    || httpServletRequest.getRequestURI().startsWith("/v3/api-docs")) {
                chain.doFilter(request, response);
                return;
            }
            // prometheus
            if (httpServletRequest.getRequestURI().startsWith("/actuator/prometheus")) {
                chain.doFilter(request, response);
                return;
            }
            if (httpServletRequest.getRequestURI().startsWith("/api/consumer/pubres")) {
                chain.doFilter(request, response);
                return;
            }
            if (httpServletRequest.getRequestURI().startsWith("/druid")) {
                chain.doFilter(request, response);
                return;
            }

            // 解析traceId
            String traceId = httpServletRequest.getHeader(TRACE_ID);
            if (StringUtils.isBlank(traceId)) {
                traceId = UUID.randomUUID().toString().replaceAll("-", "");
            }
            MDC.put(TRACE_ID, traceId);

            if (httpServletRequest.getRequestURI().startsWith("/api/open")) {
                chain.doFilter(request, response);
                return;
            }
            if (httpServletRequest.getRequestURI().startsWith("/api/chatroom/") && !httpServletRequest.getRequestURI().equals("/api/chatroom/login")) {
                String token = httpServletRequest.getHeader("token");
                RBucket<String> bucket = redissonClient.getBucket("chatroomLoginToken:" + token);
                boolean exists = bucket.isExists();
                if (!exists) {
                    writeError((HttpServletResponse) response, "token已过期");
                    return;
                }
                String userID = bucket.get();
                if (StringUtils.isEmpty(userID)) {
                    writeError((HttpServletResponse) response, "token已过期");
                    return;
                }
                bucket.set(userID, Duration.ofHours(1));
                MDC.put(HttpContextFilter.UID, userID);

                Session session = new Session(userID, userID, "chatroom", SourceEnum.CHATROOM.name());
                Session.setSession(session);

                chain.doFilter(request, response);
                return;
            }

            String userID = (String)request.getAttribute("userID");
            if (StringUtils.isBlank(userID)) {
                userID = (String) httpServletRequest.getSession().getAttribute("userID");
            }
            if (StringUtils.isEmpty(userID)) {
                // 限制api url
                if (httpServletRequest.getRequestURI().startsWith("/api/buyEmailOrder/open")) {
                    // 从 header 获取 token
                    String token = httpServletRequest.getHeader("Authorization");
                    if (StrUtil.isBlank(token)) {
                        token = httpServletRequest.getHeader("token");
                    }

                    if (StrUtil.isNotBlank(token)) {
                        String ip = IpUtil.getIpAddr(httpServletRequest);
                        int maxFailCount = 10; // 最大失败次数

                        // 根据ip查询失败次数,
                        int failCount = userService.getFailCountByIp(ip);
                        if (failCount >= maxFailCount) {
                            log.warn("ip:{},超过最大失败次数: {},禁止访问", ip, maxFailCount);
                            writeError((HttpServletResponse)response, "超过最大失败次数，请稍后再试");
                            return;
                        }

                        // 通过 token 查询用户并设置 session
                        User user = userService.findUserByUserApiKey(token);
                        if (user == null) {
                            log.warn("ip:{},无效的 token: {}", ip,token);
                            // 记录失败次数,失败10次,禁止1小时
                            userService.incrementFailCount(ip);
                            writeError((HttpServletResponse)response, "无效的 token，请重新登录");
                            return;
                        }
                        // 清理ip
                        userService.clearFailCount(ip);
                        userID = user.getUserID();
                    }
                }

                MDC.put(UID,userID);

                Session session = new Session(userID, userID, "openApi", SourceEnum.API.name());
                Session.setSession(session);

                chain.doFilter(request, response);
            } else {
                MDC.put(UID,userID);

                Session session = new Session(httpServletRequest.getSession());
                Session.setSession(session);

                chain.doFilter(request, response);
            }
        } catch (Exception e) {
            log.error("HttpContextFilter error", e);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(UID);
            Session.removeSession();
        }
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\": 401, \"message\": \"" + message + "\"}");
        response.flushBuffer();
    }
}
