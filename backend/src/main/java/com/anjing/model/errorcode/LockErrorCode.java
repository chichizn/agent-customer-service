package com.anjing.model.errorcode;

/**
 * 分布式锁错误码枚举
 * 
 * <p>用于分布式锁相关的系统异常场景</p>
 * 
 * <h3>🎯 设计原则：</h3>
 * <ul>
 *   <li>🔒 系统异常 - 属于基础设施层面问题</li>
 *   <li>📝 统一编码 - 1500-1599 分布式锁相关</li>
 *   <li>🎨 场景覆盖 - 涵盖锁的完整生命周期</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
public enum LockErrorCode implements ErrorCode {

    // 分布式锁相关 1500-1599
    LOCK_KEY_MISSING("1500", "锁的key不能为空"),
    LOCK_ACQUIRE_FAILED("1501", "获取锁失败"),
    LOCK_TIMEOUT("1502", "获取锁超时"),
    LOCK_RELEASE_FAILED("1503", "释放锁失败"),
    LOCK_CONFIG_ERROR("1504", "锁配置错误"),
    LOCK_EXPRESSION_ERROR("1505", "锁key表达式解析错误"),
    LOCK_REDIS_ERROR("1506", "Redis连接异常"),
    LOCK_INTERRUPTED("1507", "锁等待被中断");

    private final String code;
    private final String message;

    LockErrorCode(String code, String message) {
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
