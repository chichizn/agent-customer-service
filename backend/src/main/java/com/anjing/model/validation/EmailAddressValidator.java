package com.anjing.model.validation;

import com.anjing.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 📧 邮箱校验器
 * 
 * <p>实现EmailAddress注解的校验逻辑</p>
 * 
 * @author Backend Template Team
 * @version 1.0
 * @since 1.0.0
 */
public class EmailAddressValidator implements ConstraintValidator<EmailAddress, String> {

    private boolean required;

    @Override
    public void initialize(EmailAddress constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果不是必填且值为空，则通过校验
        if (!required && (value == null || value.trim().isEmpty())) {
            return true;
        }
        
        // 如果是必填但值为空，则校验失败
        if (required && (value == null || value.trim().isEmpty())) {
            return false;
        }
        
        // 使用ValidationUtils进行格式校验
        return ValidationUtils.isValidEmail(value.trim());
    }
}
