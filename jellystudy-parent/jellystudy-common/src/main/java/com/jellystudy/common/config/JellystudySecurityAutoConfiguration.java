package com.jellystudy.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.common.auth.BearerTokenResolver;
import com.jellystudy.common.auth.JellystudyAuthProperties;
import com.jellystudy.common.auth.JellystudyBearerWriteAuthFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@ConditionalOnWebApplication
@EnableWebSecurity
@EnableConfigurationProperties({JellystudySecurityProperties.class, JellystudyCorsProperties.class, JellystudyAuthProperties.class})
public class JellystudySecurityAutoConfiguration {

    @Bean
    SecurityFilterChain jellystudySecurityFilterChain(
            HttpSecurity http,
            JellystudySecurityProperties props,
            JellystudyAuthProperties authProps,
            ObjectProvider<BearerTokenResolver> tokenResolver,
            ObjectMapper objectMapper) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.cors(Customizer.withDefaults());
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        BearerTokenResolver resolver = tokenResolver.getIfAvailable();
        if (authProps.isBearerWriteEnabled() && resolver != null) {
            http.addFilterBefore(
                    new JellystudyBearerWriteAuthFilter(resolver, authProps, objectMapper),
                    UsernamePasswordAuthenticationFilter.class);
        }

        boolean requireKey = props.isEnabled()
                && props.getApiKey() != null
                && !props.getApiKey().isBlank();

        if (!requireKey) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/api/health", "/error").permitAll()
                .anyRequest().authenticated());
        http.addFilterBefore(new ApiKeyAuthFilter(props.getApiKey()), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
