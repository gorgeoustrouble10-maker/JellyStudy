package com.jellystudy.qa.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 状态码
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

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 成功响应（带消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 400错误
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return error(400, message);
    }

    /**
     * 404错误
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return error(404, message);
    }

    /**
     * 500错误
     */
    public static <T> ApiResponse<T> serverError(String message) {
        return error(500, message);
    }
}
