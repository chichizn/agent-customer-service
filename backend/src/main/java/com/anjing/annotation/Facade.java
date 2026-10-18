package com.anjing.annotation;

import java.lang.annotation.*;

/**
 * 🎯 统一校验注解 - 一个注解搞定校验+日志+异常处理
 * 
 * <h3>🚀 什么时候用？</h3>
 * <ul>
 *   <li>📡 <b>RPC服务</b> - Dubbo服务的方法上</li>
 *   <li>🔧 <b>Service业务方法</b> - 核心业务逻辑需要校验时</li>
 *   <li>🔗 <b>模块间调用</b> - 不同模块之间的接口方法</li>
 * </ul>
 * 
 * <h3>❌ 什么时候不用？</h3>
 * <ul>
 *   <li>🌐 <b>Controller方法</b> - 用@Valid就够了</li>
 *   <li>🛠️ <b>工具方法</b> - 简单的工具类方法</li>
 *   <li>⚡ <b>高频方法</b> - 调用非常频繁的方法</li>
 * </ul>
 * 
 * <h3>📝 基础用法：</h3>
 * <pre>
 * // 最简单的用法
 * {@code @Facade}
 * public OrderVO createOrder(OrderRequest request) {
 *     // 自动校验request里的@NotNull、@NotBlank等注解
 *     // 自动记录调用日志和执行时间
 *     // 校验失败自动返回错误信息
 * }
 * </pre>
 * 
 * <h3>🎛️ 高级用法 - 分组校验：</h3>
 * <pre>
 * // 创建用户：只校验创建时需要的字段
 * {@code @Facade(validationGroups = ValidationGroups.Create.class)}
 * public UserVO createUser(UserRequest request) {...}
 * 
 * // 更新用户：只校验更新时需要的字段  
 * {@code @Facade(validationGroups = ValidationGroups.Update.class)}
 * public UserVO updateUser(UserRequest request) {...}
 * </pre>
 * 
 * <h3>🔧 配置参数：</h3>
 * <ul>
 *   <li><b>scene</b> - 业务场景描述，用于日志</li>
 *   <li><b>enableValidation</b> - 是否校验参数，默认true</li>
 *   <li><b>enableLogging</b> - 是否记录日志，默认true</li>
 *   <li><b>validationGroups</b> - 校验分组，支持不同场景不同规则</li>
 * </ul>
 * 
 * <h3>💡 完整示例：</h3>
 * <pre>
 * {@code @Facade(
 *     scene = "用户注册",
 *     validationGroups = ValidationGroups.Register.class,
 *     enableLogging = true
 * )}
 * public APIResponse&lt;UserVO&gt; registerUser(UserRequest request) {
 *     // 1. 自动校验request参数
 *     // 2. 记录详细日志：🚀 [Facade] 开始执行方法: registerUser | 场景: 用户注册
 *     // 3. 如果校验失败，自动返回错误响应
 *     // 4. 记录执行时间：✅ [Facade] 方法执行成功 | 耗时: 156ms
 *     return userService.doRegister(request);
 * }
 * </pre>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Facade {
    
    /**
     * 是否启用参数校验
     * 
     * @return 默认true，设为false可关闭校验
     */
    boolean enableValidation() default true;
    
    /**
     * 是否启用方法调用日志
     * 
     * @return 默认true，设为false可关闭日志
     */
    boolean enableLogging() default true;
    
    /**
     * 校验分组 - 支持不同场景不同规则
     * 
     * <p>例如：同一个UserRequest，创建时校验密码，更新时不校验密码</p>
     * <pre>
     * // 创建时需要密码
     * {@code @NotBlank(groups = ValidationGroups.Create.class)}
     * private String password;
     * 
     * // 使用分组校验
     * {@code @Facade(validationGroups = ValidationGroups.Create.class)}
     * </pre>
     * 
     * @return 校验分组类数组，默认使用所有校验规则
     */
    Class<?>[] validationGroups() default {};
    
    /**
     * 业务场景描述
     * 
     * <p>用于日志记录，帮助定位问题</p>
     * <p>例如：scene = "用户注册" 会在日志中显示为 "场景: 用户注册"</p>
     * 
     * @return 场景描述
     */
    String scene() default "";
}
