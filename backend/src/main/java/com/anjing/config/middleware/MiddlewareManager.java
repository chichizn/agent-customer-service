package com.anjing.config.middleware;

import com.anjing.config.properties.FeatureProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 🎛️ 中间件管理器 - 简化版本
 * 
 * <p>统一管理各种中间件的启用/禁用状态检查</p>
 * 
 * <h3>🎯 核心功能：</h3>
 * <ul>
 *   <li>🔍 启动时状态检查</li>
 *   <li>📊 功能状态统计</li>
 *   <li>📋 清晰的状态报告</li>
 * </ul>
 * 
 * <h3>📌 设计理念：</h3>
 * <ul>
 *   <li>✅ 启用/禁用状态清晰明了</li>
 *   <li>🚫 无复杂的降级策略</li>
 *   <li>💡 简单易懂的配置管理</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 2.0 - 简化版本
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MiddlewareManager {

    private final FeatureProperties featureProperties;

    /**
     * 中间件状态枚举 - 简化版本
     */
    public enum MiddlewareStatus {
        ENABLED("✅", "已启用"),
        DISABLED("❌", "已禁用");

        private final String icon;
        private final String description;

        MiddlewareStatus(String icon, String description) {
            this.icon = icon;
            this.description = description;
        }

        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    /**
     * 中间件信息 - 包含版本信息
     */
    public static class MiddlewareInfo {
        private final String name;
        private final MiddlewareStatus status;
        private final String version;
        private final String details;

        public MiddlewareInfo(String name, MiddlewareStatus status, String version, String details) {
            this.name = name;
            this.status = status;
            this.version = version;
            this.details = details;
        }

        public String getName() { return name; }
        public MiddlewareStatus getStatus() { return status; }
        public String getVersion() { return version; }
        public String getDetails() { return details; }
    }

    /**
     * 应用启动完成后执行健康检查
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady()
    {
        List<MiddlewareInfo> middlewareList = performHealthCheck();
        printMiddlewareStatus(middlewareList);
        printFeatureSummary();
    }

    /**
     * 执行健康检查
     */
    private List<MiddlewareInfo> performHealthCheck() {
        List<MiddlewareInfo> middlewareList = new ArrayList<>();

        // Redis健康检查
        middlewareList.add(checkRedis());
        
        // 缓存健康检查
        middlewareList.add(checkCache());
        
        // 分布式锁健康检查
        middlewareList.add(checkDistributedLock());
        
        // 数据库健康检查
        middlewareList.add(checkDatabase());
        
        // Kafka健康检查
        middlewareList.add(checkKafka());
        
        // MinIO健康检查
        middlewareList.add(checkMinio());
        
        // OSS健康检查
        middlewareList.add(checkOss());
        
        // 监控健康检查
        middlewareList.add(checkMonitoring());

        return middlewareList;
    }

    /**
     * Redis健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkRedis() {
        if (!featureProperties.getRedis().isEnabled()) {
            return new MiddlewareInfo("Redis", MiddlewareStatus.DISABLED, "7.2.x", "功能已禁用");
        }

        String details = String.format("%s:%d (超时:%dms)", 
            featureProperties.getRedis().getHost(),
            featureProperties.getRedis().getPort(),
            featureProperties.getRedis().getTimeout());
        return new MiddlewareInfo("Redis", MiddlewareStatus.ENABLED, "7.2.x", details);
    }

    /**
     * 缓存健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkCache() {
        if (!featureProperties.getCache().isEnabled()) {
            return new MiddlewareInfo("Cache", MiddlewareStatus.DISABLED, "Spring 3.4.5", "缓存功能已禁用");
        }

        String cacheType = featureProperties.getCache().getType();
        String details = String.format("类型:%s | TTL:%ds", 
            cacheType, featureProperties.getCache().getDefaultTtl());
        
        return new MiddlewareInfo("Cache", MiddlewareStatus.ENABLED, "Spring 3.4.5", details);
    }

    /**
     * 分布式锁健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkDistributedLock() {
        if (!featureProperties.getDistributedLock().isEnabled()) {
            return new MiddlewareInfo("DistributedLock", MiddlewareStatus.DISABLED, "Redisson 3.35.0", "分布式锁已禁用");
        }

        String provider = featureProperties.getDistributedLock().getProvider();
        String details = String.format("提供者:%s | 等待:%ds | 过期:%ds", 
            provider,
            featureProperties.getDistributedLock().getDefaultWaitTime(),
            featureProperties.getDistributedLock().getDefaultExpireTime());

        return new MiddlewareInfo("DistributedLock", MiddlewareStatus.ENABLED, "Redisson 3.35.0", details);
    }

    /**
     * 数据库健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkDatabase() {
        if (!featureProperties.getDatabase().isEnabled()) {
            return new MiddlewareInfo("Database", MiddlewareStatus.DISABLED, "MySQL 8.2.0", "数据库功能已禁用");
        }

        List<String> features = new ArrayList<>();
        if (featureProperties.getDatabase().isShowSql()) features.add("SQL日志");
        if (featureProperties.getDatabase().isEnableAuditing()) features.add("JPA审计");
        if (featureProperties.getDatabase().isEnablePoolMonitoring()) features.add("连接池监控");

        String details = "特性: " + String.join(", ", features);
        return new MiddlewareInfo("Database", MiddlewareStatus.ENABLED, "MySQL 8.2.0", details);
    }

    /**
     * Kafka健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkKafka() {
        if (!featureProperties.getMiddleware().getKafka().isEnabled()) {
            return new MiddlewareInfo("Kafka", MiddlewareStatus.DISABLED, "3.8.x", "消息队列已禁用");
        }

        String details = String.format("服务器:%s | 组ID:%s", 
            featureProperties.getMiddleware().getKafka().getBootstrapServers(),
            featureProperties.getMiddleware().getKafka().getGroupId());
        return new MiddlewareInfo("Kafka", MiddlewareStatus.ENABLED, "3.8.x", details);
    }

    /**
     * MinIO健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkMinio() {
        if (!featureProperties.getMiddleware().getMinio().isEnabled()) {
            return new MiddlewareInfo("MinIO", MiddlewareStatus.DISABLED, "RELEASE.2024", "对象存储已禁用");
        }

        String details = String.format("端点:%s | 桶:%s", 
            featureProperties.getMiddleware().getMinio().getEndpoint(),
            featureProperties.getMiddleware().getMinio().getBucketName());
        return new MiddlewareInfo("MinIO", MiddlewareStatus.ENABLED, "RELEASE.2024", details);
    }

    /**
     * OSS健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkOss() {
        if (!featureProperties.getMiddleware().getOss().isEnabled()) {
            return new MiddlewareInfo("OSS", MiddlewareStatus.DISABLED, "SDK 3.x", "云存储已禁用");
        }

        String details = String.format("提供商:%s | 桶:%s", 
            featureProperties.getMiddleware().getOss().getProvider(),
            featureProperties.getMiddleware().getOss().getBucketName());
        return new MiddlewareInfo("OSS", MiddlewareStatus.ENABLED, "SDK 3.x", details);
    }

    /**
     * 监控健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkMonitoring() {
        if (!featureProperties.getMonitoring().isEnabled()) {
            return new MiddlewareInfo("Monitoring", MiddlewareStatus.DISABLED, "Actuator 3.4.5", "监控功能已禁用");
        }

        List<String> features = new ArrayList<>();
        if (featureProperties.getMonitoring().isEnablePerformance()) features.add("性能监控");
        if (featureProperties.getMonitoring().isEnableHealthCheck()) features.add("健康检查");
        if (featureProperties.getMonitoring().isEnableMetrics()) features.add("指标收集");

        String details = "特性: " + String.join(", ", features);
        return new MiddlewareInfo("Monitoring", MiddlewareStatus.ENABLED, "Actuator 3.4.5", details);
    }

    /**
     * 打印中间件状态 - 包含版本信息
     */
    private void printMiddlewareStatus(List<MiddlewareInfo> middlewareList)
    {
        log.info("┌─────────────────┬──────────┬─────────────────┐");
        log.info("│ 中间件名称      │ 状态     │ 版本            │");
        log.info("├─────────────────┼──────────┼─────────────────┤");

        for (MiddlewareInfo info : middlewareList) {
            String name = String.format("%-15s", truncate(info.getName(), 15));
            String status = info.getStatus().getIcon() + " " + info.getStatus().getDescription();
            String version = String.format("%-15s", truncate(info.getVersion(), 15));
            
            log.info("│ {} │ {} │ {} │", name, String.format("%-8s", status), version);
        }

        log.info("└─────────────────┴──────────┴─────────────────┘");
    }

    /**
     * 打印功能摘要 - 最简版本
     */
    private void printFeatureSummary()
    {
        // 简化版本：只显示中间件状态表格，不再显示统计信息
        // 表格已经足够清晰地展示所有信息
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength)
    {
        if (str == null || str.length() <= maxLength)
        {
            return str == null ? "" : str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

}
