package com.jellystudy.qa.exception;

/**
 * 资源未找到异常
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, String id) {
        super(String.format("%s不存在，ID: %s", resourceType, id));
    }
}
