package com.anjing.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JSON工具类
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Slf4j
@Component
public class JsonUtils {

    private static ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        JsonUtils.objectMapper = objectMapper;
    }

    private JsonUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 对象转JSON字符串
     * 
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败: {}", obj.getClass().getSimpleName(), e);
            throw new RuntimeException("对象转JSON失败", e);
        }
    }

    /**
     * JSON字符串转对象
     * 
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败: json={}, class={}", json, clazz.getSimpleName(), e);
            throw new RuntimeException("JSON转对象失败", e);
        }
    }

    /**
     * JSON字符串转对象（复杂类型）
     * 
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 对象
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败: json={}, type={}", json, typeReference.getType(), e);
            throw new RuntimeException("JSON转对象失败", e);
        }
    }

    /**
     * 对象转JSON字符串（美化格式）
     * 
     * @param obj 对象
     * @return 美化的JSON字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转美化JSON失败: {}", obj.getClass().getSimpleName(), e);
            throw new RuntimeException("对象转美化JSON失败", e);
        }
    }

    /**
     * 检查字符串是否为有效的JSON
     * 
     * @param json JSON字符串
     * @return 是否有效
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 深拷贝对象（通过JSON序列化和反序列化）
     * 
     * @param obj   源对象
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 拷贝的对象
     */
    public static <T> T deepCopy(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        
        String json = toJson(obj);
        return fromJson(json, clazz);
    }
}
