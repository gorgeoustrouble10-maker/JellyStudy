package com.jellystudy.coach.config;

import com.jellystudy.coach.auth.CoachAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class CoachWebConfig implements WebMvcConfigurer {

    private final CoachAuthInterceptor coachAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(coachAuthInterceptor)
                .addPathPatterns("/api/coach/**");
    }
}
