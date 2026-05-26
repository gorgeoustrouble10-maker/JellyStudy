package com.jellystudy.gateway.filter;

import com.jellystudy.gateway.config.GatewayAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gateway 全局鉴权：校验 {@code X-API-Key}。
 * <p>
 * 开发环境 {@code jellystudy.gateway.auth.enabled=false}（默认）时不拦截；
 * 生产可在环境变量开启 {@code JELLYSTUDY_GATEWAY_AUTH_ENABLED=true}。
 */
@Component
@RequiredArgsConstructor
public class ApiKeyGatewayFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/gateway/ping",
            "/actuator/"
    );

    private final GatewayAuthProperties authProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!authProperties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getPath().value();
        if (isPublic(path)) {
            return chain.filter(exchange);
        }
        String expected = authProperties.getApiKey();
        if (expected == null || expected.isBlank()) {
            return unauthorized(exchange, "Gateway auth enabled but api-key is empty");
        }
        String provided = exchange.getRequest().getHeaders().getFirst(authProperties.getHeaderName());
        if (expected.equals(provided)) {
            return chain.filter(exchange);
        }
        return unauthorized(exchange, "Invalid or missing " + authProperties.getHeaderName());
    }

    private static boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        byte[] body = ("{\"success\":false,\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
