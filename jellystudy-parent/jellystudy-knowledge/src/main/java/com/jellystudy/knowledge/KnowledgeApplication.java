package com.jellystudy.knowledge;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 知识点应用启动类
 */
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class,
        org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class
})
@EnableDubbo
public class KnowledgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeApplication.class, args);
    }
}
