package com.anjing.annotation;

import java.lang.annotation.*;

/**
 * 分布式锁注解
 * 
 * 使用示例：
 * ```java
 * @DistributeLock(scene = "payment", keyExpression = "#orderId", waitTime = 10, expireTime = 30)
 * public void processPayment(String orderId) {
 *     // 业务逻辑
 * }
 * ```
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributeLock {

    /**
     * 锁的场景名称，用于区分不同业务场景的锁
     * 例如：payment、order、user等
     * 
     * @return 场景名称
     */
    String scene() default "";

    /**
     * 锁的key，支持SpEL表达式
     * 例如：#userId、#orderId、'fixed_key'等
     * 
     * @return 锁的key表达式
     */
    String keyExpression() default "";

    /**
     * 锁的固定key（当不使用SpEL表达式时）
     * 
     * @return 固定key
     */
    String key() default "";

    /**
     * 等待锁的时间（秒）
     * -1表示使用默认值
     * 
     * @return 等待时间
     */
    long waitTime() default -1;

    /**
     * 锁的过期时间（秒）
     * -1表示使用默认值
     * 
     * @return 过期时间
     */
    long expireTime() default -1;
}
