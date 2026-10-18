package com.anjing.model.exception;

import com.anjing.model.errorcode.ErrorCode;

/**
 * 系统异常
 * 
 * <p>因为系统原因导致的异常，比如超时、网络异常、数据库连接异常等</p>
 * 
 * <h3>🎯 设计原则：</h3>
 * <ul>
 *   <li>🔒 强制约束 - 构造函数必须传入ErrorCode</li>
 *   <li>⚠️ 系统级别 - 区别于业务异常，属于系统层面问题</li>
 *   <li>🎨 枚举管理 - 通过枚举约束所有可能的系统错误码</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
public class SystemException extends RuntimeException {

    private ErrorCode errorCode;

    public SystemException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SystemException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SystemException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public SystemException(Throwable cause, ErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public SystemException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, ErrorCode errorCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}