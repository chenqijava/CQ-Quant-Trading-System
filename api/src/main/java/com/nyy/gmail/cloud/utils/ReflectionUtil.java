package com.nyy.gmail.cloud.utils;

import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.response.ResultCode;

import java.lang.reflect.Field;
import java.util.Map;

public class ReflectionUtil {
    public static void setFields(Map<String, Object> fieldMap, Object targetObject) {
        Class<?> clazz = targetObject.getClass();

        for (Map.Entry<String, Object> entry : fieldMap.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(targetObject, fieldValue);
            } catch (NoSuchFieldException e) {
                throw new CommonException(ResultCode.ERROR, targetObject.getClass().getSimpleName() + " non-existent filter name:" + fieldName);
            } catch (IllegalAccessException e) {
                throw new CommonException(ResultCode.ERROR, targetObject.getClass().getSimpleName() + " set fail filter name:" + fieldName);
            }
        }
    }

    public static void setProperties(Map<String, String> filtersMap, Object targetObject) {
        Class<?> clazz = targetObject.getClass();

        for (Map.Entry<String, String> entry : filtersMap.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();

            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);

                Class<?> fieldType = field.getType();
                Object convertedValue = convertValue(fieldValue, fieldType);
                if (convertedValue == null) {
                    continue;
                }

                field.set(targetObject, convertedValue);
            } catch (NoSuchFieldException e) {
                throw new CommonException(ResultCode.ERROR, targetObject.getClass().getSimpleName() + " non-existent filter name:" + fieldName);
            } catch (IllegalAccessException e) {
                throw new CommonException(ResultCode.ERROR, targetObject.getClass().getSimpleName() + " set fail filter name:" + fieldName);
            }
        }
    }

    private static Object convertValue(String value, Class<?> targetType) {
        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            }
        } catch (Exception e) {
            return null;
        }
        throw new IllegalArgumentException("Unsupported type: " + targetType);
    }
}
