package com.jellystudy.coach.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class AuthTokenStoreTestcontainersTest {

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private AuthTokenStore tokenStore;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(), redis.getMappedPort(6379));
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        tokenStore = new AuthTokenStore(template);
    }

    @Test
    void issueAndResolveToken() {
        String token = tokenStore.issueToken("32308117");
        assertEquals("32308117", tokenStore.resolveUsername(token).orElseThrow());
    }

    @Test
    void revokeRemovesToken() {
        String token = tokenStore.issueToken("demo");
        tokenStore.revoke(token);
        assertTrue(tokenStore.resolveUsername(token).isEmpty());
    }
}
