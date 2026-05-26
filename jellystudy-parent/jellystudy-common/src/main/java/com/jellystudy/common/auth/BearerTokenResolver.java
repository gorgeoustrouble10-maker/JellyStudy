package com.jellystudy.common.auth;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

/**
 * 从 Redis 解析 Coach 签发的 Bearer Token → username。
 */
public class BearerTokenResolver {

    private final StringRedisTemplate redis;

    public BearerTokenResolver(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Optional<String> resolveUsername(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return Optional.empty();
        }
        String username = redis.opsForValue().get(AuthTokenConstants.TOKEN_PREFIX + bearerToken);
        return Optional.ofNullable(username).filter(s -> !s.isBlank());
    }

    public static String stripBearer(String authorizationHeader) {
        if (authorizationHeader == null) {
            return "";
        }
        return authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7).trim()
                : authorizationHeader.trim();
    }
}
