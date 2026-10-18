package com.anjing.util;

import java.util.regex.Pattern;

/**
 * 验证工具类
 * 
 * 提供常用的数据验证方法
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
public class ValidationUtils {

    /**
     * 手机号正则表达式
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /**
     * 身份证号正则表达式
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");

    /**
     * 用户名正则表达式（字母、数字、下划线）
     */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    /**
     * IP地址正则表达式
     */
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    /**
     * URL正则表达式
     */
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");

    private ValidationUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 验证手机号
     * 
     * @param phone 手机号
     * @return 是否有效
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 验证邮箱
     * 
     * @param email 邮箱
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 验证身份证号
     * 
     * @param idCard 身份证号
     * @return 是否有效
     */
    public static boolean isValidIdCard(String idCard) {
        if (idCard == null || !ID_CARD_PATTERN.matcher(idCard).matches()) {
            return false;
        }
        
        // 验证校验码
        return validateIdCardChecksum(idCard);
    }

    /**
     * 验证用户名
     * 
     * @param username 用户名
     * @return 是否有效
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * 验证IP地址
     * 
     * @param ip IP地址
     * @return 是否有效
     */
    public static boolean isValidIp(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }

    /**
     * 验证URL
     * 
     * @param url URL
     * @return 是否有效
     */
    public static boolean isValidUrl(String url) {
        return url != null && URL_PATTERN.matcher(url).matches();
    }

    /**
     * 验证密码强度
     * 
     * @param password 密码
     * @return 强度等级（1-5）
     */
    public static int getPasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            return 1;
        }
        
        int score = 0;
        
        // 长度检查
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        
        // 字符类型检查
        if (password.matches(".*[a-z].*")) score++;  // 小写字母
        if (password.matches(".*[A-Z].*")) score++;  // 大写字母
        if (password.matches(".*[0-9].*")) score++;  // 数字
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++; // 特殊字符
        
        return Math.min(score, 5);
    }

    /**
     * 验证密码是否符合要求
     * 
     * @param password 密码
     * @return 是否符合要求
     */
    public static boolean isValidPassword(String password) {
        return getPasswordStrength(password) >= 3;
    }

    /**
     * 验证数字范围
     * 
     * @param value 值
     * @param min   最小值
     * @param max   最大值
     * @return 是否在范围内
     */
    public static boolean isInRange(Number value, Number min, Number max) {
        if (value == null) {
            return false;
        }
        
        double val = value.doubleValue();
        double minVal = min != null ? min.doubleValue() : Double.MIN_VALUE;
        double maxVal = max != null ? max.doubleValue() : Double.MAX_VALUE;
        
        return val >= minVal && val <= maxVal;
    }

    /**
     * 验证字符串长度
     * 
     * @param str       字符串
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 是否符合长度要求
     */
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) {
            return minLength <= 0;
        }
        
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 验证身份证校验码
     * 
     * @param idCard 身份证号
     * @return 校验码是否正确
     */
    private static boolean validateIdCardChecksum(String idCard) {
        char[] chars = idCard.toCharArray();
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checksums = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (chars[i] - '0') * weights[i];
        }
        
        char expectedChecksum = checksums[sum % 11];
        char actualChecksum = Character.toUpperCase(chars[17]);
        
        return expectedChecksum == actualChecksum;
    }
}
