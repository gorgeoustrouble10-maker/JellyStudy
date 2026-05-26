package com.jellystudy.coach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "coach.model")
public class CoachModelProperties {

    private String apiKey = "";
    private String modelName = "qwen-turbo";
    private long timeout = 60000;
}
