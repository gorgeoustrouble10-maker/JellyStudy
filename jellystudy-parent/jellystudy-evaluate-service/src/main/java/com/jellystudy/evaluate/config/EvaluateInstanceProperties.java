package com.jellystudy.evaluate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jellystudy.instance")
public class EvaluateInstanceProperties {

    private String id = "evaluate-unknown";
}
