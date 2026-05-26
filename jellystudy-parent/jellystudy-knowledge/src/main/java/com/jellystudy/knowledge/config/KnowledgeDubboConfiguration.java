package com.jellystudy.knowledge.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 显式注册 Dubbo ApplicationConfig，避免 STRICT 模式下偶发「Default config not found」。
 */
@Configuration
public class KnowledgeDubboConfiguration {

    @Bean
    public ApplicationConfig knowledgeApplicationConfig() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName("jellystudy-knowledge-provider");
        config.setQosEnable(false);
        config.setRegisterMode("instance");
        return config;
    }
}
