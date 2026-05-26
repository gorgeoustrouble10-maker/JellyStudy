package com.jellystudy.common.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
@EnableConfigurationProperties(JellystudyAuthProperties.class)
public class JellystudyAuthAutoConfiguration {

    @Bean
    BearerTokenResolver bearerTokenResolver(StringRedisTemplate redis) {
        return new BearerTokenResolver(redis);
    }
}
