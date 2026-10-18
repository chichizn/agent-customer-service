package com.anjing.model.constants;

/**
 * 用户相关常量
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
public class UserConstants {

    /**
     * 默认头像
     */
    public static final String DEFAULT_AVATAR = "/default/avatar.png";

    /**
     * 用户名最小长度
     */
    public static final int USERNAME_MIN_LENGTH = 3;

    /**
     * 用户名最大长度
     */
    public static final int USERNAME_MAX_LENGTH = 50;

    /**
     * 密码最小长度
     */
    public static final int PASSWORD_MIN_LENGTH = 6;

    /**
     * 密码最大长度
     */
    public static final int PASSWORD_MAX_LENGTH = 32;

    /**
     * 昵称最大长度
     */
    public static final int NICKNAME_MAX_LENGTH = 50;

    /**
     * 手机号正则表达式
     */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 邮箱正则表达式
     */
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    /**
     * 用户名正则表达式（字母、数字、下划线）
     */
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9_]+$";

    /**
     * 密码加密盐值长度
     */
    public static final int SALT_LENGTH = 16;

    /**
     * 登录失败最大次数
     */
    public static final int MAX_LOGIN_ATTEMPTS = 5;

    /**
     * 账户锁定时间（分钟）
     */
    public static final int ACCOUNT_LOCK_MINUTES = 30;

    /**
     * Token过期时间（小时）
     */
    public static final int TOKEN_EXPIRE_HOURS = 24;

    /**
     * 刷新Token过期时间（天）
     */
    public static final int REFRESH_TOKEN_EXPIRE_DAYS = 7;

    private UserConstants() {
        // 工具类，禁止实例化
    }
}
