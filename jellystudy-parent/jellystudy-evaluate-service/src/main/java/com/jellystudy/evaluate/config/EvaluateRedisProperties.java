package com.jellystudy.evaluate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jellystudy.redis")
public class EvaluateRedisProperties {

    private String questionEvalPrefix = "jelly:eval:question:";
    private String answerEvalPrefix = "jelly:eval:answer:";
    private int evalCacheTtlMinutes = 10;
}
