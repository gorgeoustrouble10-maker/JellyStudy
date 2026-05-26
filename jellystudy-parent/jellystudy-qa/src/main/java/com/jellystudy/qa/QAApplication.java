package com.jellystudy.qa;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 问答应用启动类
 */
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class,
        org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class
})
@EnableDubbo
@EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
public class QAApplication {

    public static void main(String[] args) {
        SpringApplication.run(QAApplication.class, args);
    }
}
