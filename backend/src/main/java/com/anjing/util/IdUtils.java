package com.anjing.util;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成工具类
 * 
 * 提供多种ID生成策略：
 * 1. 雪花算法ID（分布式唯一）
 * 2. UUID（全局唯一）
 * 3. 时间戳ID（可读性好）
 * 4. 随机字符串（验证码等）
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Slf4j
public class IdUtils {

    /**
     * 雪花算法相关常量
     */
    private static final long EPOCH = 1640995200000L; // 2022-01-01 00:00:00
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);
    private static final long MAX_DATA_CENTER_ID = -1L ^ (-1L << DATA_CENTER_ID_BITS);
    private static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);
    
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    /**
     * 雪花算法实例
     */
    private static final SnowflakeIdGenerator SNOWFLAKE = new SnowflakeIdGenerator(1, 1);

    /**
     * 序列号生成器
     */
    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong(1);

    /**
     * 安全随机数生成器
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 字符集定义
     */
    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUMERIC = NUMBERS + LETTERS;

    private IdUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 生成雪花算法ID
     * 
     * @return 雪花算法ID
     */
    public static Long nextId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 生成雪花算法ID字符串
     * 
     * @return 雪花算法ID字符串
     */
    public static String nextIdStr() {
        return String.valueOf(SNOWFLAKE.nextId());
    }

    /**
     * 生成UUID
     * 
     * @return UUID字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成不带分隔符的UUID
     * 
     * @return 不带分隔符的UUID字符串
     */
    public static String simpleUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成时间戳ID
     * 格式：yyyyMMddHHmmss + 3位序列号
     * 
     * @return 时间戳ID
     */
    public static String timestampId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long sequence = SEQUENCE_GENERATOR.getAndIncrement() % 1000;
        return timestamp + String.format("%03d", sequence);
    }

    /**
     * 生成随机数字字符串
     * 
     * @param length 长度
     * @return 随机数字字符串
     */
    public static String randomNumbers(int length) {
        return randomString(NUMBERS, length);
    }

    /**
     * 生成随机字母字符串
     * 
     * @param length 长度
     * @return 随机字母字符串
     */
    public static String randomLetters(int length) {
        return randomString(LETTERS, length);
    }

    /**
     * 生成随机字母数字字符串
     * 
     * @param length 长度
     * @return 随机字母数字字符串
     */
    public static String randomAlphanumeric(int length) {
        return randomString(ALPHANUMERIC, length);
    }

    /**
     * 生成验证码（数字）
     * 
     * @param length 长度
     * @return 验证码
     */
    public static String generateVerificationCode(int length) {
        return randomNumbers(length);
    }

    /**
     * 生成订单号
     * 格式：前缀 + 时间戳 + 随机数
     * 
     * @param prefix 前缀
     * @return 订单号
     */
    public static String generateOrderNo(String prefix) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = randomNumbers(4);
        return prefix + timestamp + random;
    }

    /**
     * 生成随机字符串
     * 
     * @param charset 字符集
     * @param length  长度
     * @return 随机字符串
     */
    private static String randomString(String charset, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(charset.length());
            sb.append(charset.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 雪花算法ID生成器
     */
    private static class SnowflakeIdGenerator {
        private final long workerId;
        private final long dataCenterId;
        private long sequence = 0L;
        private long lastTimestamp = -1L;

        public SnowflakeIdGenerator(long workerId, long dataCenterId) {
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException("工作节点ID超出范围: " + workerId);
            }
            if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
                throw new IllegalArgumentException("数据中心ID超出范围: " + dataCenterId);
            }
            this.workerId = workerId;
            this.dataCenterId = dataCenterId;
        }

        public synchronized long nextId() {
            long timestamp = System.currentTimeMillis();

            if (timestamp < lastTimestamp) {
                throw new RuntimeException("时钟回拨，拒绝生成ID");
            }

            if (lastTimestamp == timestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    timestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0L;
            }

            lastTimestamp = timestamp;

            return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                    | (dataCenterId << DATA_CENTER_ID_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence;
        }

        private long tilNextMillis(long lastTimestamp) {
            long timestamp = System.currentTimeMillis();
            while (timestamp <= lastTimestamp) {
                timestamp = System.currentTimeMillis();
            }
            return timestamp;
        }
    }
}
