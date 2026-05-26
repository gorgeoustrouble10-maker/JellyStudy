package com.jellystudy.coach.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue evaluationCompletedQueue(@Value("${coach.rabbit.evaluation-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Queue quizGenerateQueue(@Value("${coach.rabbit.quiz-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
