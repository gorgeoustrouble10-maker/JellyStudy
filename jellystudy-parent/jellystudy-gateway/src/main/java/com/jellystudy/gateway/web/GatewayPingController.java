package com.jellystudy.gateway.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Gateway 自检接口（不转发到下游），用于 curl / 监控探活。
 * GET /api/gateway/ping
 */
@RestController
public class GatewayPingController {

    @GetMapping("/api/gateway/ping")
    public Mono<Map<String, Object>> ping() {
        return Mono.just(Map.of(
                "service", "jellystudy-gateway",
                "status", "ok",
                "timestamp", Instant.now().toString(),
                "hint", "鉴权关闭时可直接访问；开启后需在 Header 带 X-API-Key"
        ));
    }
}
