package com.jellystudy.common.config;

import com.jellystudy.common.health.HealthProbe;
import com.jellystudy.common.health.RedisHealthProbe;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
public class RedisHealthProbeAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    HealthProbe redisHealthProbe(RedisConnectionFactory connectionFactory) {
        return new RedisHealthProbe(connectionFactory);
    }
}
