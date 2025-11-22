package com.nyy.gmail.cloud.utils;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public abstract class PropertiesUtil {
    public static String readProperty(String code) {
        Properties props = new Properties();
        InputStream resource = PropertiesUtil.class.getClassLoader().getResourceAsStream("application.properties");
        try (InputStreamReader reader = new InputStreamReader(resource, "UTF-8")) {
            props.load(reader);
        } catch (IOException e) {
        }
        return props.getProperty(code, "");
    }
}
