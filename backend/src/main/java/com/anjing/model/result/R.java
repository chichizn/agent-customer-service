package com.anjing.model.result;

import lombok.Data;

/**
 * 通用响应结果封装类
 * 简化版统一响应格式
 */
@Data
public class R<T> {

    /**
     * 状态码 (0=成功, 非0=失败)
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> R<T> ok(T data) {
        return new R<>(0, "操作成功", data);
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> R<T> ok() {
        return new R<>(0, "操作成功", null);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> R<T> ok(String message, T data) {
        return new R<>(0, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> R<T> fail(String message) {
        return new R<>(-1, message, null);
    }

    /**
     * 失败响应（带错误码）
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return code == 0;
    }
}

