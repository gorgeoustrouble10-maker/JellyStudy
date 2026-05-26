package com.jellystudy.coach.auth;

import com.jellystudy.common.auth.AuthTokenConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthTokenStore {

    private static final String PREFIX = AuthTokenConstants.TOKEN_PREFIX;
    private static final Duration TTL = Duration.ofHours(168);

    private final StringRedisTemplate redis;

    public String issueToken(String username) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(PREFIX + token, username, TTL);
        return token;
    }

    public Optional<String> resolveUsername(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String username = redis.opsForValue().get(PREFIX + token);
        return Optional.ofNullable(username).filter(s -> !s.isBlank());
    }

    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            redis.delete(PREFIX + token);
        }
    }
}
