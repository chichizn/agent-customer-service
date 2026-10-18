package com.anjing.model.errorcode;

/**
 * 🔄 状态机错误码枚举
 * 
 * <p>专用于状态机操作相关的错误码，包括状态转换失败、非法状态等</p>
 * 
 * <h3>📋 错误码分类：</h3>
 * <ul>
 *   <li><b>1900-1999</b> - 状态机相关错误</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 * @since 1.0.0
 */
public enum StateMachineErrorCode implements ErrorCode {

    // ==================== 状态机相关错误 1900-1999 ====================
    
    /**
     * 状态转换失败
     */
    STATE_TRANSITION_FAILED("1900", "状态转换失败"),
    
    /**
     * 非法状态转换
     */
    ILLEGAL_STATE_TRANSITION("1901", "非法状态转换"),
    
    /**
     * 当前状态不支持该操作
     */
    STATE_NOT_SUPPORT_OPERATION("1902", "当前状态不支持该操作"),
    
    /**
     * 状态机未初始化
     */
    STATE_MACHINE_NOT_INITIALIZED("1903", "状态机未初始化"),
    
    /**
     * 状态转换条件不满足
     */
    STATE_TRANSITION_CONDITION_NOT_MET("1904", "状态转换条件不满足"),
    
    /**
     * 状态机配置错误
     */
    STATE_MACHINE_CONFIG_ERROR("1905", "状态机配置错误"),
    
    /**
     * 状态转换监听器执行失败
     */
    STATE_TRANSITION_LISTENER_FAILED("1906", "状态转换监听器执行失败"),
    
    /**
     * 状态或事件为空
     */
    STATE_OR_EVENT_IS_NULL("1907", "状态或事件不能为空"),
    
    /**
     * 重复的状态转换定义
     */
    DUPLICATE_STATE_TRANSITION("1908", "重复的状态转换定义"),
    
    /**
     * 状态机实例不存在
     */
    STATE_MACHINE_INSTANCE_NOT_FOUND("1909", "状态机实例不存在");

    private final String code;
    private final String message;

    StateMachineErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
