package com.jellystudy.common.config;

import com.jellystudy.common.health.HealthProbe;
import com.jellystudy.common.health.RabbitHealthProbe;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = RabbitAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.connection.ConnectionFactory")
public class RabbitHealthProbeAutoConfiguration {

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    HealthProbe rabbitHealthProbe(ConnectionFactory connectionFactory) {
        return new RabbitHealthProbe(connectionFactory);
    }
}
