package com.anjing.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 日期工具类
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
public class DateUtils {

    public static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    public static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATETIME_PATTERN);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN);

    private DateUtils() {
        // 工具类，禁止实例化
    }

    /**
     * LocalDateTime转字符串
     * 
     * @param dateTime 时间
     * @return 字符串
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * LocalDateTime转字符串（自定义格式）
     * 
     * @param dateTime 时间
     * @param pattern  格式
     * @return 字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 字符串转LocalDateTime
     * 
     * @param dateTimeStr 时间字符串
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }

    /**
     * 字符串转LocalDateTime（自定义格式）
     * 
     * @param dateTimeStr 时间字符串
     * @param pattern     格式
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Date转LocalDateTime
     * 
     * @param date Date对象
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * LocalDateTime转Date
     * 
     * @param dateTime LocalDateTime对象
     * @return Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取当前时间字符串
     * 
     * @return 当前时间字符串
     */
    public static String now() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    /**
     * 获取当前日期字符串
     * 
     * @return 当前日期字符串
     */
    public static String today() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }

    /**
     * 计算两个时间之间的差值（秒）
     * 
     * @param start 开始时间
     * @param end   结束时间
     * @return 秒数
     */
    public static long betweenSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * 计算两个时间之间的差值（分钟）
     * 
     * @param start 开始时间
     * @param end   结束时间
     * @return 分钟数
     */
    public static long betweenMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 计算两个时间之间的差值（小时）
     * 
     * @param start 开始时间
     * @param end   结束时间
     * @return 小时数
     */
    public static long betweenHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个时间之间的差值（天数）
     * 
     * @param start 开始时间
     * @param end   结束时间
     * @return 天数
     */
    public static long betweenDays(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 判断是否为今天
     * 
     * @param dateTime 时间
     * @return 是否为今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return dateTime.toLocalDate().equals(now.toLocalDate());
    }

    /**
     * 获取一天的开始时间
     * 
     * @param dateTime 时间
     * @return 一天的开始时间
     */
    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * 获取一天的结束时间
     * 
     * @param dateTime 时间
     * @return 一天的结束时间
     */
    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }
}
