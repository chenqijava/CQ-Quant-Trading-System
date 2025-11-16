package com.nyy.gmail.cloud.common.configuration;

import com.nyy.gmail.cloud.utils.GeoIPCountryUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoIPAutoConfig {

    @PostConstruct
    public void initGeoIP() throws Exception {
        GeoIPCountryUtils.init();  // 项目启动时初始化
    }
}