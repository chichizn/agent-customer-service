package com.anjing.model.response;

import lombok.Data;

/**
 * 统一API响应结果
 */
@Data
public class APIResponse<T>
{

    /**
     * 成功状态码
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 失败状态码
     */
    public static final String ERROR_CODE = "-1";

    /**
     * 响应码
     */
    private String code;

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
    private Long timestamp;

    public APIResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public APIResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public APIResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 判断是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }

    /**
     * 成功响应
     * 
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> success(T data) {
        return new APIResponse<>(SUCCESS_CODE, "操作成功", data);
    }

    /**
     * 成功响应
     * 
     * @param data    数据
     * @param message 消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> success(T data, String message) {
        return new APIResponse<>(SUCCESS_CODE, message, data);
    }

    /**
     * 成功响应（无数据）
     * 
     * @return 响应结果
     */
    public static <T> APIResponse<T> success() {
        return new APIResponse<>(SUCCESS_CODE, "操作成功", null);
    }

    /**
     * 成功响应（无数据）
     * 
     * @param message 消息
     * @return 响应结果
     */
    public static <T> APIResponse<T> success(String message) {
        return new APIResponse<>(SUCCESS_CODE, message, null);
    }

    /**
     * 错误响应
     * 
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> error(String message) {
        return new APIResponse<>(ERROR_CODE, message);
    }

    /**
     * 错误响应
     * 
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> error(String code, String message) {
        return new APIResponse<>(code, message);
    }

    /**
     * 错误响应
     * 
     * @param code    错误码
     * @param message 错误消息
     * @param data    数据
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> error(String code, String message, T data) {
        return new APIResponse<>(code, message, data);
    }
}
