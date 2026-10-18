package com.anjing.model.validation;

/**
 * 🎛️ 校验分组 - 让同一个字段在不同场景有不同校验规则
 * 
 * <h3>🤔 什么是分组校验？</h3>
 * <p>同一个Request类，在不同业务场景下校验不同的字段。</p>
 * <p>比如：用户注册时需要密码，用户更新时不需要密码。</p>
 * 
 * <h3>📝 在Request类中使用：</h3>
 * <pre>
 * public class UserRequest {
 *     // ID只在更新时校验
 *     {@code @NotNull(groups = ValidationGroups.Update.class, message = "更新时ID不能为空")}
 *     private Long id;
 * 
 *     // 密码只在创建时校验
 *     {@code @NotBlank(groups = ValidationGroups.Create.class, message = "创建时密码不能为空")}
 *     private String password;
 * 
 *     // 用户名在创建和更新时都要校验
 *     {@code @NotBlank(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})}
 *     private String username;
 * }
 * </pre>
 * 
 * <h3>🎯 在方法中使用：</h3>
 * <pre>
 * // 创建用户：只校验password和username，不校验id
 * {@code @Facade(validationGroups = ValidationGroups.Create.class)}
 * public UserVO createUser(UserRequest request) {...}
 * 
 * // 更新用户：只校验id和username，不校验password
 * {@code @Facade(validationGroups = ValidationGroups.Update.class)}
 * public UserVO updateUser(UserRequest request) {...}
 * </pre>
 * 
 * <h3>🔧 手动校验时使用：</h3>
 * <pre>
 * // 按分组校验
 * BeanValidator.validateObject(userRequest, ValidationGroups.Create.class);
 * BeanValidator.validateObject(userRequest, ValidationGroups.Update.class);
 * </pre>
 * 
 * <h3>💡 可用的分组：</h3>
 * <ul>
 *   <li><b>Create</b> - 创建操作（通常校验所有必填字段）</li>
 *   <li><b>Update</b> - 更新操作（通常需要ID，其他字段可选）</li>
 *   <li><b>Query</b> - 查询操作（通常只校验查询条件）</li>
 *   <li><b>Delete</b> - 删除操作（通常只需要ID）</li>
 *   <li><b>Register</b> - 注册操作（可能比Create校验更严格）</li>
 *   <li><b>Login</b> - 登录操作（用户名+密码）</li>
 *   <li>还有更多...</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
public interface ValidationGroups {

    /**
     * 🆕 创建操作校验分组
     * 
     * <p>用于新建记录时的参数校验，通常需要校验所有必填字段</p>
     */
    interface Create {}

    /**
     * 🔄 更新操作校验分组
     * 
     * <p>用于更新记录时的参数校验，通常需要校验ID和要更新的字段</p>
     */
    interface Update {}

    /**
     * 🔍 查询操作校验分组
     * 
     * <p>用于查询操作时的参数校验，通常校验查询条件的合法性</p>
     */
    interface Query {}

    /**
     * 🗑️ 删除操作校验分组
     * 
     * <p>用于删除操作时的参数校验，通常只需要校验ID等关键字段</p>
     */
    interface Delete {}

    /**
     * 📊 批量操作校验分组
     * 
     * <p>用于批量操作时的参数校验，可能有特殊的校验规则</p>
     */
    interface Batch {}

    /**
     * 🔐 登录操作校验分组
     * 
     * <p>用于登录相关操作的参数校验</p>
     */
    interface Login {}

    /**
     * 👤 注册操作校验分组
     * 
     * <p>用于用户注册操作的参数校验</p>
     */
    interface Register {}

    /**
     * 🔄 状态变更校验分组
     * 
     * <p>用于状态变更操作的参数校验，如启用/禁用等</p>
     */
    interface StatusChange {}

    /**
     * 🔒 权限操作校验分组
     * 
     * <p>用于权限相关操作的参数校验</p>
     */
    interface Permission {}

    /**
     * 💰 金额相关校验分组
     * 
     * <p>用于涉及金额计算的操作校验，通常有更严格的校验规则</p>
     */
    interface Money {}
}
