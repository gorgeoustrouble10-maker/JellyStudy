package com.jellystudy.coach.config;

import com.jellystudy.coach.health.DashscopeHealthProbe;
import com.jellystudy.common.health.HealthProbe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashscopeHealthProbeConfiguration {

    @Bean
    HealthProbe dashscopeHealthProbe(CoachModelProperties modelProperties) {
        return new DashscopeHealthProbe(modelProperties);
    }
}
