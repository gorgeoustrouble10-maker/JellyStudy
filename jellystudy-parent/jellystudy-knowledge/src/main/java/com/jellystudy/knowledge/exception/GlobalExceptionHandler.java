package com.jellystudy.knowledge.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.badRequest(message));
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("资源未找到: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(ex.getMessage()));
    }

    /**
     * 处理Dubbo调用异常
     */
    @ExceptionHandler(org.apache.dubbo.rpc.RpcException.class)
    public ResponseEntity<ApiResponse<Void>> handleRpcException(org.apache.dubbo.rpc.RpcException ex) {
        log.error("Dubbo调用异常: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, "服务调用失败: " + ex.getMessage()));
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("系统异常: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("系统内部错误，请稍后重试"));
    }
}
