package com.jellystudy.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jellystudy.gateway.auth")
public class GatewayAuthProperties {

    /** 默认 false：本地开发不要求 API Key */
    private boolean enabled = false;

    /** 与下游服务 ApiKeyAuthFilter 使用相同请求头 */
    private String headerName = "X-API-Key";

    private String apiKey = "";
}
