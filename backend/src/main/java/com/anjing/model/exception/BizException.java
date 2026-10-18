package com.anjing.model.exception;

import com.anjing.model.errorcode.ErrorCode;

/**
 * 业务异常
 * 
 * <p>按照标准实现的业务异常类，强制使用ErrorCode约束错误码</p>
 * 
 * <h3>🎯 设计原则：</h3>
 * <ul>
 *   <li>🔒 强制约束 - 构造函数必须传入ErrorCode</li>
 *   <li>📝 统一规范 - 所有业务异常都有明确的错误码</li>
 *   <li>🎨 枚举管理 - 通过枚举约束所有可能的错误码</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
public class BizException extends RuntimeException
{

    private ErrorCode errorCode;

    public BizException(ErrorCode errorCode)
    {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(String message, ErrorCode errorCode)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public BizException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BizException(Throwable cause, ErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public BizException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, ErrorCode errorCode) {
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