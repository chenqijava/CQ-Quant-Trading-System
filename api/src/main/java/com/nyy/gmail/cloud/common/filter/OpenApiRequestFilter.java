package com.nyy.gmail.cloud.common.filter;

import com.nyy.gmail.cloud.utils.IpUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.enums.SourceEnum;
import com.nyy.gmail.cloud.entity.mysql.ApiKey;
import com.nyy.gmail.cloud.service.ApiKeyService;
import com.nyy.gmail.cloud.utils.SignGenerator;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(3)
@WebFilter(urlPatterns = "/api/open/*")
public class OpenApiRequestFilter implements Filter {
    @Autowired
    private ApiKeyService apiKeyService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!httpRequest.getRequestURI().startsWith("/api/open")) {
            chain.doFilter(request, response);
            return;
        }
        if (httpRequest.getRequestURI().startsWith("/api/open/v1beta")) {
            chain.doFilter(request, response);
            return;
        }

        String ipAddr = IpUtil.getIpAddr((HttpServletRequest) request);
        if (ipAddr != null && ipAddr.contains(",")) {
            ipAddr = ipAddr.split(",")[0].trim();
        }

        String xApiKey = httpRequest.getHeader("X-API-KEY");
        String xSign = httpRequest.getHeader("X-SIGN");
        if (StringUtils.isBlank(xApiKey) || StringUtils.isBlank(xSign)) {
            writeError(httpResponse, "API Key or Sign is required");
            return;
        }
        ApiKey apiKey = apiKeyService.getApiKey(xApiKey);
        if (apiKey == null) {
            writeError(httpResponse, "API Key is invalid");
            return;
        }
        if (StringUtils.isNotEmpty(apiKey.getWhiteIp())) {
            if (!Arrays.asList(apiKey.getWhiteIp().split(",")).contains(ipAddr)) {
                writeError(httpResponse, "["+ipAddr+"] not in white ips, please add it");
                return;
            }
        }
        CachedBodyHttpServletRequest wrapper = new CachedBodyHttpServletRequest(httpRequest);
        byte[] body = StreamUtils.copyToByteArray(wrapper.getInputStream());
        JSONObject json = JSON.parseObject(body);
        String sign = SignGenerator.generateSign(json, apiKey.getApiSecret());
        log.info("OpenApiRequestFilter[{}] userID: {}, x-api-key: {}, x-sign: {}, sign: {}", httpRequest.getRequestURI(), apiKey.getUserID(), xApiKey, xSign, sign);
        if (!xSign.equals(sign)) {
            writeError(httpResponse, "Sign is invalid");
            return;
        }
        String userID = apiKey.getUserID();
        MDC.put(HttpContextFilter.UID, userID);

        Session session = new Session(userID, userID, "openApi", SourceEnum.API.name());
        Session.setSession(session);

        chain.doFilter(wrapper, response);
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\": 401, \"message\": \"" + message + "\"}");
        response.flushBuffer();
    }

    public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
            
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }
    
                @Override
                public boolean isReady() {
                    return true;
                }
    
                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }
    
                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }
            };
        }
    
        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }
    }
}
