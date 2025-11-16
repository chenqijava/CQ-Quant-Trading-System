package com.nyy.gmail.cloud.common.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "config")
public class PathConfig {

    private String res;

    private String resBak;

    private String uploadDir;

    private String ipPlatform;

}
