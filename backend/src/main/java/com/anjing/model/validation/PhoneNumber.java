package com.anjing.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 📱 手机号校验注解 - 校验中国大陆手机号格式
 * 
 * <h3>📝 基础用法：</h3>
 * <pre>
 * public class UserRequest {
 *     {@code @PhoneNumber(message = "手机号格式不正确")}
 *     private String mobile;
 * }
 * </pre>
 * 
 * <h3>🎛️ 可选字段：</h3>
 * <pre>
 * public class UserRequest {
 *     // 可以为空，但不为空时必须是正确格式
 *     {@code @PhoneNumber(required = false, message = "联系电话格式不正确")}
 *     private String contactPhone;
 * }
 * </pre>
 * 
 * <h3>📱 支持的手机号：</h3>
 * <ul>
 *   <li>13x xxxx xxxx</li>
 *   <li>14x xxxx xxxx</li>
 *   <li>15x xxxx xxxx</li>
 *   <li>16x xxxx xxxx</li>
 *   <li>17x xxxx xxxx</li>
 *   <li>18x xxxx xxxx</li>
 *   <li>19x xxxx xxxx</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
@Documented
public @interface PhoneNumber {
    
    /**
     * 校验失败时的错误消息
     */
    String message() default "手机号格式不正确";
    
    /**
     * 校验分组
     */
    Class<?>[] groups() default {};
    
    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 是否必填
     * 
     * <p>如果设置为false，则空值（null或空字符串）会通过校验</p>
     * 
     * @return 是否必填，默认true
     */
    boolean required() default true;
}
