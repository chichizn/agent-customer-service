package com.anjing.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 🎛️ 功能特性配置类
 * 
 * <p>统一管理脚手架的各种功能开关，支持条件化启用/禁用功能模块</p>
 * 
 * <h3>🎯 设计目标：</h3>
 * <ul>
 *   <li>🔧 灵活的功能开关控制</li>
 *   <li>🚀 优雅的启动降级机制</li>
 *   <li>📊 清晰的功能状态展示</li>
 *   <li>🔌 易于扩展的配置结构</li>
 * </ul>
 * 
 * <h3>📋 配置示例：</h3>
 * <pre>
 * app:
 *   features:
 *     redis:
 *       enabled: true
 *       host: localhost
 *       port: 6379
 *     cache:
 *       enabled: true
 *       type: redis  # redis/memory
 *     distributed-lock:
 *       enabled: true
 *       provider: redisson  # redisson/local
 *     database:
 *       enabled: true
 *       show-sql: true
 *     middleware:
 *       kafka:
 *         enabled: false
 *       minio:
 *         enabled: false
 *       oss:
 *         enabled: false
 * </pre>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.features")
public class FeatureProperties {

    /**
     * Redis功能配置
     */
    private RedisFeature redis = new RedisFeature();

    /**
     * 缓存功能配置
     */
    private CacheFeature cache = new CacheFeature();

    /**
     * 分布式锁功能配置
     */
    private DistributedLockFeature distributedLock = new DistributedLockFeature();

    /**
     * 数据库功能配置
     */
    private DatabaseFeature database = new DatabaseFeature();

    /**
     * 中间件功能配置
     */
    private MiddlewareFeature middleware = new MiddlewareFeature();

    /**
     * 监控功能配置
     */
    private MonitoringFeature monitoring = new MonitoringFeature();

    /**
     * Redis功能配置
     */
    @Data
    public static class RedisFeature {
        /**
         * 是否启用Redis
         */
        private boolean enabled = true;

        /**
         * Redis主机地址
         */
        private String host = "localhost";

        /**
         * Redis端口
         */
        private int port = 6379;

        /**
         * 连接超时时间（毫秒）
         */
        private int timeout = 5000;

        /**
         * 是否启用集群模式
         */
        private boolean cluster = false;
    }

    /**
     * 缓存功能配置
     */
    @Data
    public static class CacheFeature {
        /**
         * 是否启用缓存
         */
        private boolean enabled = true;

        /**
         * 缓存类型：redis/memory
         */
        private String type = "redis";

        /**
         * 默认过期时间（秒）
         */
        private long defaultTtl = 3600;

        /**
         * 是否启用缓存统计
         */
        private boolean enableStats = true;
    }

    /**
     * 分布式锁功能配置
     */
    @Data
    public static class DistributedLockFeature {
        /**
         * 是否启用分布式锁
         */
        private boolean enabled = true;

        /**
         * 锁提供者：redisson/local
         */
        private String provider = "redisson";

        /**
         * 默认等待时间（秒）
         */
        private long defaultWaitTime = 10;

        /**
         * 默认过期时间（秒）
         */
        private long defaultExpireTime = 30;
    }

    /**
     * 数据库功能配置
     */
    @Data
    public static class DatabaseFeature {
        /**
         * 是否启用数据库
         */
        private boolean enabled = true;

        /**
         * 是否显示SQL
         */
        private boolean showSql = true;

        /**
         * 是否启用JPA审计
         */
        private boolean enableAuditing = true;

        /**
         * 是否启用连接池监控
         */
        private boolean enablePoolMonitoring = true;
    }

    /**
     * 中间件功能配置
     */
    @Data
    public static class MiddlewareFeature {
        /**
         * Kafka配置
         */
        private KafkaConfig kafka = new KafkaConfig();

        /**
         * MinIO配置
         */
        private MinioConfig minio = new MinioConfig();

        /**
         * OSS配置
         */
        private OssConfig oss = new OssConfig();

        @Data
        public static class KafkaConfig {
            private boolean enabled = false;
            private String bootstrapServers = "localhost:9092";
            private String groupId = "agent-dev-scaffolding";
        }

        @Data
        public static class MinioConfig {
            private boolean enabled = false;
            private String endpoint = "http://localhost:9000";
            private String accessKey = "";
            private String secretKey = "";
            private String bucketName = "default";
        }

        @Data
        public static class OssConfig {
            private boolean enabled = false;
            private String provider = "aliyun"; // aliyun/tencent/aws
            private String endpoint = "";
            private String accessKey = "";
            private String secretKey = "";
            private String bucketName = "";
        }
    }

    /**
     * 监控功能配置
     */
    @Data
    public static class MonitoringFeature {
        /**
         * 是否启用监控
         */
        private boolean enabled = true;

        /**
         * 是否启用性能监控
         */
        private boolean enablePerformance = true;

        /**
         * 是否启用健康检查
         */
        private boolean enableHealthCheck = true;

        /**
         * 是否启用指标收集
         */
        private boolean enableMetrics = true;
    }

    /**
     * 获取功能启用状态摘要
     */
    public String getFeatureSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Redis: ").append(redis.enabled ? "✅" : "❌").append(" | ");
        summary.append("Cache: ").append(cache.enabled ? "✅" : "❌").append(" | ");
        summary.append("DistLock: ").append(distributedLock.enabled ? "✅" : "❌").append(" | ");
        summary.append("Database: ").append(database.enabled ? "✅" : "❌").append(" | ");
        summary.append("Kafka: ").append(middleware.kafka.enabled ? "✅" : "❌").append(" | ");
        summary.append("MinIO: ").append(middleware.minio.enabled ? "✅" : "❌").append(" | ");
        summary.append("OSS: ").append(middleware.oss.enabled ? "✅" : "❌").append(" | ");
        summary.append("Monitoring: ").append(monitoring.enabled ? "✅" : "❌");
        return summary.toString();
    }

    /**
     * 获取启用的功能数量
     */
    public int getEnabledFeatureCount() {
        int count = 0;
        if (redis.enabled) count++;
        if (cache.enabled) count++;
        if (distributedLock.enabled) count++;
        if (database.enabled) count++;
        if (middleware.kafka.enabled) count++;
        if (middleware.minio.enabled) count++;
        if (middleware.oss.enabled) count++;
        if (monitoring.enabled) count++;
        return count;
    }

    /**
     * 检查是否有中间件启用
     */
    public boolean hasMiddlewareEnabled() {
        return middleware.kafka.enabled || middleware.minio.enabled || middleware.oss.enabled;
    }
}
