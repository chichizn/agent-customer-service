package com.anjing.model.errorcode;

/**
 * 认证授权错误码枚举
 * 
 * <p>用于登录权限业务场景的专用错误码</p>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
public enum AuthErrorCode implements ErrorCode {

    // 登录相关 2100-2199
    LOGIN_FAILED("2100", "登录失败"),
    USERNAME_OR_PASSWORD_ERROR("2101", "用户名或密码错误"),
    USER_NOT_FOUND("2102", "用户不存在"),
    USER_DISABLED("2103", "用户已被禁用"),
    USER_LOCKED("2104", "用户已被锁定"),
    CAPTCHA_ERROR("2105", "验证码错误"),
    LOGIN_EXPIRED("2106", "登录已过期"),

    // 权限相关 2200-2299
    PERMISSION_DENIED("2200", "权限不足"),
    ROLE_NOT_FOUND("2201", "角色不存在"),
    PERMISSION_NOT_FOUND("2202", "权限不存在"),
    USER_ROLE_NOT_FOUND("2203", "用户角色关系不存在"),

    // Token相关 2300-2399
    TOKEN_GENERATE_FAILED("2300", "Token生成失败"),
    TOKEN_PARSE_FAILED("2301", "Token解析失败"),
    TOKEN_SIGNATURE_INVALID("2302", "Token签名无效"),
    REFRESH_TOKEN_INVALID("2303", "刷新Token无效"),
    REFRESH_TOKEN_EXPIRED("2304", "刷新Token已过期");

    private final String code;
    private final String message;

    AuthErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
