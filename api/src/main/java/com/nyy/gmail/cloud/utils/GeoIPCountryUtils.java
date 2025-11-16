package com.nyy.gmail.cloud.utils;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class GeoIPCountryUtils {

    private static DatabaseReader reader;
    private static final Map<String, String> COUNTRY_CACHE = new HashMap<>();

    @PostConstruct
    public static void init() throws IOException {
        Resource resource;
        resource = new ClassPathResource("geoip/GeoLite2-Country.mmdb");

        try (InputStream is = resource.getInputStream()) {
            reader = new DatabaseReader.Builder(is).build(); // 直接从流加载
        }
    }

    /**
     * 查询国家代码（如 "CN", "US"）
     */
    public static String getCountryCode(String ip) {
        try {
            // 缓存检查
            String cached = COUNTRY_CACHE.get(ip);
            if (cached != null) return cached;

            // 数据库查询
            CountryResponse response = reader.country(InetAddress.getByName(ip));
            System.out.println(response);
            String countryCode = response.getCountry().getIsoCode();

            // 写入缓存
            COUNTRY_CACHE.put(ip, countryCode);
            return countryCode;

        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * 清空缓存
     */
    public static void clearCache() {
        COUNTRY_CACHE.clear();
    }
}