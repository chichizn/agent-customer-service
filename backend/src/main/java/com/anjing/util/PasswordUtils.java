package com.anjing.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码工具类
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Slf4j
public class PasswordUtils {

    private static final String SALT_PREFIX = "{salt}";
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 密码加密
     * 
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        // 生成随机盐值
        String salt = generateSalt();
        
        // 加密密码
        String encodedPassword = encodeWithSalt(rawPassword, salt);
        
        // 返回 {salt}base64(salt):md5(password+salt) 格式
        return SALT_PREFIX + Base64.getEncoder().encodeToString(salt.getBytes(StandardCharsets.UTF_8)) 
                + ":" + encodedPassword;
    }

    /**
     * 密码验证
     * 
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        try {
            if (!encodedPassword.startsWith(SALT_PREFIX)) {
                // 兼容旧版本没有盐值的密码
                return encodedPassword.equals(DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8)));
            }
            
            // 解析盐值和密码
            String content = encodedPassword.substring(SALT_PREFIX.length());
            String[] parts = content.split(":", 2);
            if (parts.length != 2) {
                return false;
            }
            
            String salt = new String(Base64.getDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String storedPassword = parts[1];
            
            // 用相同的盐值加密输入密码
            String encodedInputPassword = encodeWithSalt(rawPassword, salt);
            
            return storedPassword.equals(encodedInputPassword);
        } catch (Exception e) {
            log.error("密码验证失败", e);
            return false;
        }
    }

    /**
     * 生成随机盐值
     * 
     * @return 盐值
     */
    private static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 使用盐值加密密码
     * 
     * @param password 密码
     * @param salt     盐值
     * @return 加密后的密码
     */
    private static String encodeWithSalt(String password, String salt) {
        String saltedPassword = password + salt;
        return DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成随机密码
     * 
     * @param length 密码长度
     * @return 随机密码
     */
    public static String generateRandomPassword(int length) {
        if (length < 6) {
            throw new IllegalArgumentException("密码长度不能小于6");
        }
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(chars.length());
            password.append(chars.charAt(index));
        }
        
        return password.toString();
    }

    /**
     * 检查密码强度
     * 
     * @param password 密码
     * @return 强度等级（1-5，5为最强）
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
}
