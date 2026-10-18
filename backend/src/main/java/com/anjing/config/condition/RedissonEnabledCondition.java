package com.anjing.config.condition;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 🔒 Redisson启用条件判断
 * 
 * <p>自定义条件类，用于判断是否应该启用Redisson配置</p>
 * 
 * <h3>🎯 启用条件：</h3>
 * <ul>
 *   <li>app.features.redis.enabled=true</li>
 *   <li>app.features.distributed-lock.enabled=true (默认true)</li>
 *   <li>app.features.distributed-lock.provider=redisson (默认redisson)</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
public class RedissonEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConditionMessage.Builder message = ConditionMessage.forCondition("Redisson Enabled");
        
        // 检查Redis是否启用
        String redisEnabled = context.getEnvironment().getProperty("app.features.redis.enabled", "true");
        if (!"true".equalsIgnoreCase(redisEnabled)) {
            return ConditionOutcome.noMatch(message.because("Redis is disabled"));
        }
        
        // 检查分布式锁是否启用
        String lockEnabled = context.getEnvironment().getProperty("app.features.distributed-lock.enabled", "true");
        if (!"true".equalsIgnoreCase(lockEnabled)) {
            return ConditionOutcome.noMatch(message.because("Distributed lock is disabled"));
        }
        
        // 检查分布式锁提供者
        String lockProvider = context.getEnvironment().getProperty("app.features.distributed-lock.provider", "redisson");
        if (!"redisson".equalsIgnoreCase(lockProvider)) {
            return ConditionOutcome.noMatch(message.because("Distributed lock provider is not redisson: " + lockProvider));
        }
        
        return ConditionOutcome.match(message.because("All conditions met for Redisson"));
    }
}
