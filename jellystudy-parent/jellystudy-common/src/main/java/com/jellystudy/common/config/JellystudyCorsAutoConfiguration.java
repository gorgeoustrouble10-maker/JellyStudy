package com.jellystudy.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(JellystudyCorsProperties.class)
public class JellystudyCorsAutoConfiguration {

    @Bean
    CorsFilter jellystudyCorsFilter(JellystudyCorsProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        for (String origin : props.allowedOriginsArray()) {
            config.addAllowedOrigin(origin);
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
