package com.anjing.util;

import com.anjing.model.exception.BizException;
import com.anjing.model.errorcode.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.HibernateValidator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🔍 参数校验工具类 - 手动校验参数用这个
 * 
 * <h3>🚀 什么时候用？</h3>
 * <ul>
 *   <li>🔧 <b>复杂业务逻辑中</b> - 需要手动校验参数时</li>
 *   <li>🎛️ <b>条件校验</b> - 根据业务条件决定是否校验</li>
 *   <li>📊 <b>批量校验</b> - 需要校验多个对象时</li>
 * </ul>
 * 
 * <h3>📝 基础用法：</h3>
 * <pre>
 * // 最简单的用法 - 有错误立即抛异常
 * BeanValidator.validateObject(userRequest);
 * 
 * // 不抛异常，只返回true/false
 * if (BeanValidator.isValid(userRequest)) {
 *     // 校验通过的逻辑
 * }
 * </pre>
 * 
 * <h3>🎛️ 分组校验：</h3>
 * <pre>
 * // 只校验创建时需要的字段
 * BeanValidator.validateObject(userRequest, ValidationGroups.Create.class);
 * 
 * // 只校验更新时需要的字段
 * BeanValidator.validateObject(userRequest, ValidationGroups.Update.class);
 * </pre>
 * 
 * <h3>📋 获取所有错误信息：</h3>
 * <pre>
 * // 收集所有错误，一次性显示
 * BeanValidator.validateObjectWithAllErrors(userRequest);
 * 
 * // 或者不抛异常，获取错误列表
 * Set&lt;String&gt; errors = BeanValidator.getValidationErrors(userRequest);
 * if (!errors.isEmpty()) {
 *     log.warn("校验失败: {}", errors);
 * }
 * </pre>
 * 
 * <h3>⚠️ 重要提醒：</h3>
 * <ul>
 *   <li>🔥 <b>Validator是静态的</b> - 避免CPU飙高，不要每次都创建</li>
 *   <li>⚡ <b>默认快速失败</b> - 遇到第一个错误就停止，性能更好</li>
 *   <li>🛡️ <b>自动转换异常</b> - 校验失败会抛BizException，统一处理</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 2.0
 */
@Slf4j
public class BeanValidator {

    /**
     * 🔥 静态Validator实例 - 性能关键！
     * 
     * <p><b>重要：</b>必须定义为static变量，避免每次调用都创建新实例导致CPU飙高！</p>
     * 
     * <ul>
     *   <li>HibernateValidator - 使用Hibernate Validator实现</li>
     *   <li>failFast(true) - 快速失败模式，遇到第一个错误立即返回</li>
     *   <li>线程安全 - Validator实例是线程安全的</li>
     * </ul>
     */
    private static final Validator FAST_VALIDATOR = Validation
            .byProvider(HibernateValidator.class)
            .configure()
            .failFast(true)  // 快速失败，提升性能
            .buildValidatorFactory()
            .getValidator();

    /**
     * 完整校验的Validator实例
     * 
     * <p>用于需要收集所有校验错误的场景</p>
     */
    private static final Validator FULL_VALIDATOR = Validation
            .byProvider(HibernateValidator.class)
            .configure()
            .failFast(false)  // 完整校验，收集所有错误
            .buildValidatorFactory()
            .getValidator();

    /**
     * 工具类构造函数私有化
     */
    private BeanValidator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 🚀 快速校验对象（推荐使用）
     * 
     * <p>使用failFast模式，遇到第一个校验错误立即抛出异常，性能最优</p>
     * 
     * @param object 待校验对象
     * @param groups 校验分组（可选）
     * @throws BizException 校验失败时抛出业务异常
     * @throws IllegalArgumentException 对象为null时抛出
     */
    public static void validateObject(Object object, Class<?>... groups) {
        if (object == null) {
            log.warn("校验对象为null，跳过校验");
            throw new IllegalArgumentException("校验对象不能为null");
        }

        try {
            Set<ConstraintViolation<Object>> violations = FAST_VALIDATOR.validate(object, groups);
            if (!violations.isEmpty()) {
                ConstraintViolation<Object> firstViolation = violations.iterator().next();
                String errorMessage = buildErrorMessage(firstViolation);
                
                log.warn("参数校验失败: {} - {}", object.getClass().getSimpleName(), errorMessage);
                throw new BizException(errorMessage, CommonErrorCode.PARAM_INVALID);
            }
        } catch (ValidationException e) {
            log.error("参数校验过程中发生异常: {}", e.getMessage(), e);
            throw new BizException("参数校验失败: " + e.getMessage(), CommonErrorCode.PARAM_INVALID);
        }
    }

    /**
     * 📋 完整校验对象（收集所有错误）
     * 
     * <p>收集所有校验错误信息，适用于需要一次性展示所有错误的场景</p>
     * 
     * @param object 待校验对象
     * @param groups 校验分组（可选）
     * @throws BizException 校验失败时抛出业务异常，包含所有错误信息
     * @throws IllegalArgumentException 对象为null时抛出
     */
    public static void validateObjectWithAllErrors(Object object, Class<?>... groups) {
        if (object == null) {
            log.warn("校验对象为null，跳过校验");
            throw new IllegalArgumentException("校验对象不能为null");
        }

        try {
            Set<ConstraintViolation<Object>> violations = FULL_VALIDATOR.validate(object, groups);
            if (!violations.isEmpty()) {
                String allErrors = violations.stream()
                        .map(BeanValidator::buildErrorMessage)
                        .collect(Collectors.joining("; "));
                
                log.warn("参数校验失败: {} - 共{}个错误: {}", 
                        object.getClass().getSimpleName(), violations.size(), allErrors);
                throw new BizException(allErrors, CommonErrorCode.PARAM_INVALID);
            }
        } catch (ValidationException e) {
            log.error("参数校验过程中发生异常: {}", e.getMessage(), e);
            throw new BizException("参数校验失败: " + e.getMessage(), CommonErrorCode.PARAM_INVALID);
        }
    }

    /**
     * 🔍 安全校验（不抛异常）
     * 
     * <p>校验对象但不抛出异常，返回校验结果</p>
     * 
     * @param object 待校验对象
     * @param groups 校验分组（可选）
     * @return 校验是否通过
     */
    public static boolean isValid(Object object, Class<?>... groups) {
        if (object == null) {
            return false;
        }

        try {
            Set<ConstraintViolation<Object>> violations = FAST_VALIDATOR.validate(object, groups);
            return violations.isEmpty();
        } catch (Exception e) {
            log.error("参数校验过程中发生异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 📝 获取校验错误信息（不抛异常）
     * 
     * <p>获取所有校验错误信息，不抛出异常</p>
     * 
     * @param object 待校验对象
     * @param groups 校验分组（可选）
     * @return 校验错误信息集合，如果校验通过则返回空集合
     */
    public static Set<String> getValidationErrors(Object object, Class<?>... groups) {
        if (object == null) {
            return Set.of("校验对象不能为null");
        }

        try {
            Set<ConstraintViolation<Object>> violations = FULL_VALIDATOR.validate(object, groups);
            return violations.stream()
                    .map(BeanValidator::buildErrorMessage)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("参数校验过程中发生异常: {}", e.getMessage(), e);
            return Set.of("参数校验失败: " + e.getMessage());
        }
    }

    /**
     * 🎯 校验单个属性
     * 
     * <p>校验对象的单个属性值</p>
     * 
     * @param object 待校验对象
     * @param propertyName 属性名
     * @param groups 校验分组（可选）
     * @return 校验错误信息集合，如果校验通过则返回空集合
     */
    public static Set<String> validateProperty(Object object, String propertyName, Class<?>... groups) {
        if (object == null || propertyName == null || propertyName.trim().isEmpty()) {
            return Set.of("校验对象或属性名不能为null");
        }

        try {
            Set<ConstraintViolation<Object>> violations = FULL_VALIDATOR.validateProperty(object, propertyName, groups);
            return violations.stream()
                    .map(BeanValidator::buildErrorMessage)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("属性校验过程中发生异常: {}", e.getMessage(), e);
            return Set.of("属性校验失败: " + e.getMessage());
        }
    }

    /**
     * 构建错误信息
     * 
     * @param violation 校验违规信息
     * @return 格式化的错误信息
     */
    private static String buildErrorMessage(ConstraintViolation<Object> violation) {
        return String.format("%s: %s", violation.getPropertyPath(), violation.getMessage());
    }

    /**
     * 🎯 批量校验多个对象
     * 
     * <p>批量校验多个对象，适用于批量处理场景</p>
     * 
     * @param objects 待校验对象数组
     * @param groups 校验分组（可选）
     * @throws BizException 任何一个对象校验失败时抛出
     */
    public static void validateObjects(Object[] objects, Class<?>... groups) {
        if (objects == null || objects.length == 0) {
            return;
        }

        for (int i = 0; i < objects.length; i++) {
            try {
                validateObject(objects[i], groups);
            } catch (BizException e) {
                throw new BizException(String.format("第%d个参数校验失败: %s", i + 1, e.getMessage()), CommonErrorCode.PARAM_INVALID);
            }
        }
    }
}
