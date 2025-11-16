package com.nyy.gmail.cloud.utils;

import java.util.regex.Pattern;

/**
 * IP地址和端口校验工具类
 */
public class IPPortValidator {

    // IPv4正则（严格模式）
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // IPv6正则（支持压缩格式如 ::1）
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))$"
    );

    /**
     * 校验IPv4格式
     * @param ip 要校验的IP地址
     * @return 是否合法
     */
    public static boolean isValidIPv4(String ip) {
        return ip != null && IPV4_PATTERN.matcher(ip).matches();
    }

    /**
     * 校验IPv6格式
     * @param ip 要校验的IP地址
     * @return 是否合法
     */
    public static boolean isValidIPv6(String ip) {
        return ip != null && IPV6_PATTERN.matcher(ip).matches();
    }

    /**
     * 校验IP地址（自动识别IPv4/IPv6）
     * @param ip 要校验的IP地址
     * @return 是否合法
     */
    public static boolean isValidIP(String ip) {
        return isValidIPv4(ip) || isValidIPv6(ip);
    }

    /**
     * 校验端口号合法性
     * @param port 要校验的端口号
     * @param allowSystemPorts 是否允许系统端口（0-1023）
     * @return 是否合法
     */
    public static boolean isValidPort(int port, boolean allowSystemPorts) {
        int minPort = allowSystemPorts ? 0 : 1024;
        return port >= minPort && port <= 65535;
    }

    /**
     * 校验端口号字符串合法性
     * @param portStr 要校验的端口字符串
     * @param allowSystemPorts 是否允许系统端口（0-1023）
     * @return 是否合法
     */
    public static boolean isValidPort(String portStr, boolean allowSystemPorts) {
        try {
            int port = Integer.parseInt(portStr);
            return isValidPort(port, allowSystemPorts);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 校验IP:Port组合格式
     * @param address 要校验的地址
     * @param allowSystemPorts 是否允许系统端口
     * @return 是否合法
     */
    public static boolean isValidIPWithPort(String address, boolean allowSystemPorts) {
        if (address == null) return false;

        // 处理IPv6地址（包含方括号的情况）
        if (address.startsWith("[")) {
            int closeBracket = address.indexOf(']');
            if (closeBracket == -1) return false;

            String ip = address.substring(1, closeBracket);
            if (!isValidIPv6(ip)) return false;

            String portPart = address.substring(closeBracket + 1);
            if (!portPart.startsWith(":")) return false;

            String portStr = portPart.substring(1);
            return isValidPort(portStr, allowSystemPorts);
        } else {
            // 处理IPv4或普通格式
            int lastColon = address.lastIndexOf(':');
            if (lastColon <= 0) return false;

            String ip = address.substring(0, lastColon);
            String portStr = address.substring(lastColon + 1);

            return isValidIP(ip) && isValidPort(portStr, allowSystemPorts);
        }
    }

    /**
     * 从IP:Port字符串中提取IP和端口
     * @param address 要解析的地址
     * @return IPPort对象，包含ip和port字段，解析失败返回null
     */
    public static IPPort parseIPAndPort(String address) {
        if (!isValidIPWithPort(address, true)) return null;

        try {
            String ip;
            String portStr;

            if (address.startsWith("[")) {
                // IPv6格式 [ip]:port
                int closeBracket = address.indexOf(']');
                ip = address.substring(1, closeBracket);
                portStr = address.substring(closeBracket + 2);
            } else {
                // IPv4格式 ip:port
                int lastColon = address.lastIndexOf(':');
                ip = address.substring(0, lastColon);
                portStr = address.substring(lastColon + 1);
            }

            int port = Integer.parseInt(portStr);
            return new IPPort(ip, port);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * IP和端口封装类
     */
    public static class IPPort {
        private final String ip;
        private final int port;

        public IPPort(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            if (ip.contains(":")) {
                return "[" + ip + "]:" + port; // IPv6格式
            } else {
                return ip + ":" + port; // IPv4格式
            }
        }
    }
}