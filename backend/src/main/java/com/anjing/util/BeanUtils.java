package com.anjing.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean工具类
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Slf4j
public class BeanUtils
{

    private BeanUtils()
    {
        // 工具类，禁止实例化
    }

    /**
     * 对象属性复制
     * 
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 目标对象
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass)
    {
        if (source == null)
        {
            return null;
        }
        
        try
        {
            Constructor<T> constructor = targetClass.getDeclaredConstructor();
            T target = constructor.newInstance();
            org.springframework.beans.BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e)
        {
            log.error("对象属性复制失败: source={}, targetClass={}", 
                    source.getClass().getSimpleName(), targetClass.getSimpleName(), e);
            throw new RuntimeException("对象属性复制失败", e);
        }
    }

    /**
     * 对象属性复制（忽略指定属性）
     * 
     * @param source           源对象
     * @param targetClass      目标类型
     * @param ignoreProperties 忽略的属性名
     * @param <T>              目标类型
     * @return 目标对象
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass, String... ignoreProperties) {
        if (source == null) {
            return null;
        }
        
        try {
            Constructor<T> constructor = targetClass.getDeclaredConstructor();
            T target = constructor.newInstance();
            org.springframework.beans.BeanUtils.copyProperties(source, target, ignoreProperties);
            return target;
        } catch (Exception e) {
            log.error("对象属性复制失败: source={}, targetClass={}", 
                    source.getClass().getSimpleName(), targetClass.getSimpleName(), e);
            throw new RuntimeException("对象属性复制失败", e);
        }
    }

    /**
     * 对象属性复制（到已存在的目标对象）
     * 
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        
        try {
            org.springframework.beans.BeanUtils.copyProperties(source, target);
        } catch (Exception e) {
            log.error("对象属性复制失败: source={}, target={}", 
                    source.getClass().getSimpleName(), target.getClass().getSimpleName(), e);
            throw new RuntimeException("对象属性复制失败", e);
        }
    }

    /**
     * 列表对象属性复制
     * 
     * @param sourceList  源对象列表
     * @param targetClass 目标类型
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 目标对象列表
     */
    public static <S, T> List<T> copyPropertiesList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            T target = copyProperties(source, targetClass);
            targetList.add(target);
        }
        
        return targetList;
    }
}
