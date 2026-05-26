package com.jellystudy.common.health;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@RequiredArgsConstructor
public class RabbitHealthProbe implements HealthProbe {

    private final ConnectionFactory connectionFactory;

    @Override
    public String component() {
        return "rabbitmq";
    }

    @Override
    public boolean isUp() {
        try (var conn = connectionFactory.createConnection()) {
            return conn.isOpen();
        } catch (Exception e) {
            return false;
        }
    }
}
