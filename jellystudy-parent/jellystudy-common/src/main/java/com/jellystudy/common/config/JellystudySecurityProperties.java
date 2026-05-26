package com.jellystudy.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 可选 API Key 认证：默认关闭，便于本地演示；生产通过环境变量开启。
 */
@Data
@ConfigurationProperties(prefix = "jellystudy.security")
public class JellystudySecurityProperties {

    /**
     * 为 true 且 api-key 非空时，要求请求头 X-API-Key。
     */
    private boolean enabled = false;

    private String apiKey = "";
}
