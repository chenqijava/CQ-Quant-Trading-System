package com.nyy.gmail.cloud.utils;


import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;


public abstract class IpUtil {
    public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public static boolean isPrivateIPv4(String ip) {
        try {
            if (ip.equals("127.0.0.1") || ip.equals("localhost")) {
                return true;
            }
            byte[] addr = InetAddress.getByName(ip).getAddress();
            int first = Byte.toUnsignedInt(addr[0]);
            int second = Byte.toUnsignedInt(addr[1]);

            if (first == 10) {
                return true;
            }
            if (first == 172 && (second >= 16 && second <= 31)) {
                return true;
            }
            if (first == 192 && second == 168) {
                return true;
            }
            return false;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
