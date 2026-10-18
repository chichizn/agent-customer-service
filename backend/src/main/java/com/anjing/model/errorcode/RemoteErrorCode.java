package com.anjing.model.errorcode;


/**
 * 🌐 远程调用错误码枚举
 * 
 * <p>专用于RPC调用、HTTP调用等远程服务调用场景的错误码</p>
 * 
 * <h3>📋 错误码分类：</h3>
 * <ul>
 *   <li><b>1600-1699</b> - 远程调用基础错误</li>
 *   <li><b>1700-1799</b> - 响应校验错误</li>
 *   <li><b>1800-1899</b> - 网络和超时错误</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 * @since 1.0.0
 */
public enum RemoteErrorCode implements ErrorCode {

    // ==================== 远程调用基础错误 1600-1699 ====================
    
    /**
     * 远程调用失败
     */
    REMOTE_CALL_FAILED("1600", "远程调用失败"),
    
    /**
     * 远程服务不可用
     */
    REMOTE_SERVICE_UNAVAILABLE("1601", "远程服务不可用"),
    
    /**
     * 远程调用参数错误
     */
    REMOTE_CALL_PARAM_ERROR("1602", "远程调用参数错误"),
    
    /**
     * 远程调用方法不存在
     */
    REMOTE_METHOD_NOT_FOUND("1603", "远程调用方法不存在"),
    
    /**
     * 远程调用权限不足
     */
    REMOTE_CALL_PERMISSION_DENIED("1604", "远程调用权限不足"),

    // ==================== 响应校验错误 1700-1799 ====================
    
    /**
     * 远程调用响应为空
     */
    REMOTE_RESPONSE_NULL("1700", "远程调用响应为空"),
    
    /**
     * 远程调用响应格式错误
     */
    REMOTE_RESPONSE_FORMAT_ERROR("1701", "远程调用响应格式错误"),
    
    /**
     * 远程调用响应状态失败
     */
    REMOTE_RESPONSE_STATUS_FAILED("1702", "远程调用响应状态失败"),
    
    /**
     * 远程调用响应码错误
     */
    REMOTE_RESPONSE_CODE_ERROR("1703", "远程调用响应码错误"),
    
    /**
     * 远程调用响应数据为空
     */
    REMOTE_RESPONSE_DATA_NULL("1704", "远程调用响应数据为空"),
    
    /**
     * 远程调用响应校验失败
     */
    REMOTE_RESPONSE_VALIDATION_FAILED("1705", "远程调用响应校验失败"),

    // ==================== 网络和超时错误 1800-1899 ====================
    
    /**
     * 远程调用超时
     */
    REMOTE_CALL_TIMEOUT("1800", "远程调用超时"),
    
    /**
     * 远程调用网络异常
     */
    REMOTE_CALL_NETWORK_ERROR("1801", "远程调用网络异常"),
    
    /**
     * 远程调用连接被拒绝
     */
    REMOTE_CALL_CONNECTION_REFUSED("1802", "远程调用连接被拒绝"),
    
    /**
     * 远程调用连接超时
     */
    REMOTE_CALL_CONNECTION_TIMEOUT("1803", "远程调用连接超时"),
    
    /**
     * 远程调用读取超时
     */
    REMOTE_CALL_READ_TIMEOUT("1804", "远程调用读取超时"),
    
    /**
     * 远程调用重试次数超限
     */
    REMOTE_CALL_RETRY_EXCEEDED("1805", "远程调用重试次数超限"),
    
    /**
     * 远程调用熔断器开启
     */
    REMOTE_CALL_CIRCUIT_BREAKER_OPEN("1806", "远程调用熔断器开启，服务暂时不可用");

    private final String code;
    private final String message;

    RemoteErrorCode(String code, String message) {
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
