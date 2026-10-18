package com.anjing.config.redis;

import com.anjing.config.condition.RedissonEnabledCondition;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * 🔒 Redisson配置类 - 条件化启用
 * 
 * <p>提供分布式锁、分布式集合等功能的Redis客户端配置</p>
 * 
 * <h3>🎯 启用条件：</h3>
 * <ul>
 *   <li>app.features.redis.enabled=true</li>
 *   <li>app.features.distributed-lock.enabled=true</li>
 *   <li>app.features.distributed-lock.provider=redisson</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 2.0 - 条件化配置
 */
@Configuration
@Conditional(RedissonEnabledCondition.class)
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.timeout:3000}")
    private int timeout;

    /**
     * 配置Redisson客户端
     * 
     * @return RedissonClient实例
     */
    @Bean
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 单机模式配置
        String redisUrl = String.format("redis://%s:%d", redisHost, redisPort);
        config.useSingleServer()
                .setAddress(redisUrl)
                .setDatabase(redisDatabase)
                .setTimeout(timeout)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(10)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}
