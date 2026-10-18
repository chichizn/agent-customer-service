package com.anjing.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LoginRequest extends BaseRequest
{

    /**
     * 用户名或邮箱
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名长度不能超过100个字符")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 32, message = "密码长度不能超过32个字符")
    private String password;

    /**
     * 验证码（可选）
     */
    private String captcha;

    /**
     * 记住我（可选）
     */
    private Boolean rememberMe = false;
}
