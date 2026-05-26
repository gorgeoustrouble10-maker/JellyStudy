package com.jellystudy.common.auth;

/**
 * 与 Coach {@code AuthTokenStore} 共用的 Redis Token 前缀。
 */
public final class AuthTokenConstants {

    public static final String TOKEN_PREFIX = "jelly:auth:token:";

    private AuthTokenConstants() {
    }
}
