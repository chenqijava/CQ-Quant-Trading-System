package com.nyy.gmail.cloud.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JacksonMapUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @SuppressWarnings("rawtypes")
    public static Map toMap(Object obj) {
        return MAPPER.convertValue(obj, Map.class);
    }

    @SuppressWarnings("rawtypes")
    public static <T> T fromMap(Map map, Class<T> clazz) {
        return MAPPER.convertValue(map, clazz);
    }
}
