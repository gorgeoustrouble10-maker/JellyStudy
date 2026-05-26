package com.jellystudy.coach.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenStoreTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private AuthTokenStore store;

    @Test
    void issueTokenStoresUsernameInRedis() {
        when(redis.opsForValue()).thenReturn(valueOps);
        String token = store.issueToken("alice");
        assertEquals(32, token.length());
        verify(valueOps).set(eq("jelly:auth:token:" + token), eq("alice"), any(Duration.class));
    }

    @Test
    void resolveUsernameReturnsEmptyForBlankToken() {
        assertTrue(store.resolveUsername("  ").isEmpty());
    }

    @Test
    void resolveUsernameReturnsValueFromRedis() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("jelly:auth:token:abc")).thenReturn("bob");
        Optional<String> username = store.resolveUsername("abc");
        assertTrue(username.isPresent());
        assertEquals("bob", username.get());
    }
}
