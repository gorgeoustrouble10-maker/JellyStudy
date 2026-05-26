package com.jellystudy.evaluate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 评估引擎 Key 校验工具（引擎选择见 {@link com.jellystudy.evaluate.engine.RefreshableModelEngine}）。
 */
@Configuration
public class ModelEngineSelectorConfiguration {

    public static boolean hasValidDashScopeKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return false;
        }
        String trimmed = apiKey.trim();
        return trimmed.startsWith("sk-")
                && !trimmed.contains("请替换")
                && trimmed.length() > 12;
    }
}
