package com.jellystudy.evaluate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 评估服务 AI 模型配置（支持 Nacos 热刷新 model-name / timeout / type）。
 * API Key 仍通过环境变量注入，不入 Nacos。
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "evaluate.model")
public class EvaluateModelProperties {

    /** qianwen | mock */
    private String type = "qianwen";
    private String apiKey = "";
    private String modelName = "qwen-turbo";
    private long timeout = 60000;
    private int retryCount = 3;
}
