package com.jellystudy.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jellystudy.cors")
public class JellystudyCorsProperties {

    /**
     * 逗号分隔，例如 http://127.0.0.1:9945,http://localhost:9945
     */
    private String allowedOrigins = "http://127.0.0.1:9945,http://localhost:9945";

    public String[] allowedOriginsArray() {
        return java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
