package com.anjing.util;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 * 
 * 提供常用的字符串处理方法
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
public class StringUtils {

    private StringUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 判断字符串是否为空
     * 
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * 判断字符串是否不为空
     * 
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 判断字符串是否为空白
     * 
     * @param str 字符串
     * @return 是否为空白
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * 判断字符串是否不为空白
     * 
     * @param str 字符串
     * @return 是否不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 去除前后空格，null安全
     * 
     * @param str 字符串
     * @return 去除空格后的字符串
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * 首字母大写
     * 
     * @param str 字符串
     * @return 首字母大写的字符串
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 首字母小写
     * 
     * @param str 字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * 驼峰转下划线
     * 
     * @param str 驼峰字符串
     * @return 下划线字符串
     */
    public static String camelToSnake(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * 下划线转驼峰
     * 
     * @param str 下划线字符串
     * @return 驼峰字符串
     */
    public static String snakeToCamel(String str) {
        if (isEmpty(str)) {
            return str;
        }
        
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        
        return result.toString();
    }

    /**
     * 拼接字符串
     * 
     * @param delimiter 分隔符
     * @param elements  元素
     * @return 拼接后的字符串
     */
    public static String join(String delimiter, String... elements) {
        if (elements == null || elements.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(elements[i]);
        }
        
        return sb.toString();
    }

    /**
     * 拼接集合元素
     * 
     * @param delimiter 分隔符
     * @param elements  元素集合
     * @return 拼接后的字符串
     */
    public static String join(String delimiter, Collection<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return "";
        }
        
        return String.join(delimiter, elements);
    }

    /**
     * 重复字符串
     * 
     * @param str   字符串
     * @param count 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String str, int count) {
        if (str == null || count <= 0) {
            return "";
        }
        
        return str.repeat(count);
    }

    /**
     * 左填充
     * 
     * @param str    字符串
     * @param length 目标长度
     * @param padStr 填充字符串
     * @return 填充后的字符串
     */
    public static String leftPad(String str, int length, String padStr) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        
        int padLength = length - str.length();
        String pad = repeat(padStr, padLength / padStr.length() + 1);
        return pad.substring(0, padLength) + str;
    }

    /**
     * 右填充
     * 
     * @param str    字符串
     * @param length 目标长度
     * @param padStr 填充字符串
     * @return 填充后的字符串
     */
    public static String rightPad(String str, int length, String padStr) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        
        int padLength = length - str.length();
        String pad = repeat(padStr, padLength / padStr.length() + 1);
        return str + pad.substring(0, padLength);
    }

    /**
     * 截取字符串
     * 
     * @param str       字符串
     * @param maxLength 最大长度
     * @param suffix    后缀
     * @return 截取后的字符串
     */
    public static String truncate(String str, int maxLength, String suffix) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        
        return str.substring(0, maxLength - suffix.length()) + suffix;
    }

    /**
     * 脱敏处理
     * 
     * @param str        字符串
     * @param startKeep  开始保留字符数
     * @param endKeep    结束保留字符数
     * @param maskChar   脱敏字符
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int startKeep, int endKeep, char maskChar) {
        if (isEmpty(str) || str.length() <= startKeep + endKeep) {
            return str;
        }
        
        int maskLength = str.length() - startKeep - endKeep;
        String start = str.substring(0, startKeep);
        String end = str.substring(str.length() - endKeep);
        String mask = String.valueOf(maskChar).repeat(maskLength);
        
        return start + mask + end;
    }

    /**
     * 验证邮箱格式
     * 
     * @param email 邮箱
     * @return 是否为有效邮箱
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }

    /**
     * 验证手机号格式
     * 
     * @param phone 手机号
     * @return 是否为有效手机号
     */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        
        String phoneRegex = "^1[3-9]\\d{9}$";
        return Pattern.matches(phoneRegex, phone);
    }

    /**
     * 生成随机字符串
     * 
     * @param length 长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        
        return sb.toString();
    }
}
