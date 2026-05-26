package com.jellystudy.common.health;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@RequiredArgsConstructor
public class RedisHealthProbe implements HealthProbe {

    private final RedisConnectionFactory connectionFactory;

    @Override
    public String component() {
        return "redis";
    }

    @Override
    public boolean isUp() {
        try (var conn = connectionFactory.getConnection()) {
            return "PONG".equalsIgnoreCase(conn.ping());
        } catch (Exception e) {
            return false;
        }
    }
}
