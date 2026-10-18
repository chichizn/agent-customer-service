package com.anjing.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 📧 邮箱校验注解 - 校验邮箱地址格式
 * 
 * <h3>📝 基础用法：</h3>
 * <pre>
 * public class UserRequest {
 *     {@code @EmailAddress(message = "邮箱格式不正确")}
 *     private String email;
 * }
 * </pre>
 * 
 * <h3>🎛️ 可选字段：</h3>
 * <pre>
 * public class UserRequest {
 *     // 可以为空，但不为空时必须是正确格式
 *     {@code @EmailAddress(required = false, message = "备用邮箱格式不正确")}
 *     private String backupEmail;
 * }
 * </pre>
 * 
 * <h3>✅ 支持的邮箱格式：</h3>
 * <ul>
 *   <li>user@example.com</li>
 *   <li>user.name@example.com</li>
 *   <li>user+tag@example.com</li>
 *   <li>user123@example-domain.com</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailAddressValidator.class)
@Documented
public @interface EmailAddress {
    
    /**
     * 校验失败时的错误消息
     */
    String message() default "邮箱格式不正确";
    
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
     * @return 是否必填，默认true
     */
    boolean required() default true;
}
