package com.jellystudy.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 简单 API Key 过滤器（无状态）。
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final String expectedApiKey;

    public ApiKeyAuthFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String provided = request.getHeader(API_KEY_HEADER);
        if (expectedApiKey.equals(provided)) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "api-client", null, List.of(new SimpleGrantedAuthority("ROLE_API")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"Invalid or missing X-API-Key\"}");
    }
}
