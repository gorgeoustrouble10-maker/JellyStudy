package com.jellystudy.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 问答 / 知识点写操作 Bearer 鉴权；读接口与 Coach 路由不受影响。
 */
public class JellystudyBearerWriteAuthFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_PREFIXES = Set.of(
            "/api/questions",
            "/api/answers",
            "/api/knowledge-points");

    private final BearerTokenResolver tokenResolver;
    private final JellystudyAuthProperties authProperties;
    private final ObjectMapper objectMapper;

    public JellystudyBearerWriteAuthFilter(BearerTokenResolver tokenResolver,
                                           JellystudyAuthProperties authProperties,
                                           ObjectMapper objectMapper) {
        this.tokenResolver = tokenResolver;
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!authProperties.isBearerWriteEnabled() || !requiresWriteAuth(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = BearerTokenResolver.stripBearer(request.getHeader("Authorization"));
        var username = tokenResolver.resolveUsername(token);
        if (username.isPresent()) {
            request.setAttribute(JellystudyUserAttributes.USER_ID, username.get());
            filterChain.doFilter(request, response);
            return;
        }

        writeUnauthorized(response);
    }

    static boolean requiresWriteAuth(HttpServletRequest request) {
        if (HttpMethod.GET.matches(request.getMethod())
                || HttpMethod.HEAD.matches(request.getMethod())
                || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return WRITE_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 401);
        body.put("message", "请先登录后再进行此操作");
        body.put("data", null);
        body.put("timestamp", System.currentTimeMillis());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
