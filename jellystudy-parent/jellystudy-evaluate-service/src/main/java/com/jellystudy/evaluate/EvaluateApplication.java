package com.jellystudy.evaluate;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 评估服务启动类
 * 基于大模型的问答评估Dubbo服务
 */
@SpringBootApplication
@EnableDubbo
@EnableRetry
public class EvaluateApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvaluateApplication.class, args);
    }
}
