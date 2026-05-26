package com.jellystudy.evaluate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 配置类
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JellyStudy 评估服务 API")
                        .version("1.0.0")
                        .description("基于大模型的问答评估Dubbo服务接口文档")
                        .contact(new Contact()
                                .name("JellyStudy Team")
                                .email("support@jellystudy.com")));
    }
}
