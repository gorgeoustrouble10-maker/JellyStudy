package com.jellystudy.coach.health;

import com.jellystudy.coach.config.CoachModelProperties;
import com.jellystudy.common.health.HealthProbe;

/**
 * Coach 服务：千问 API 密钥是否已配置（不发起外网调用，避免健康检查计费）。
 */
public class DashscopeHealthProbe implements HealthProbe {

    private final CoachModelProperties modelProperties;

    public DashscopeHealthProbe(CoachModelProperties modelProperties) {
        this.modelProperties = modelProperties;
    }

    @Override
    public String component() {
        return "dashscope";
    }

    @Override
    public boolean isUp() {
        String key = modelProperties.getApiKey();
        return key != null && !key.isBlank();
    }
}
