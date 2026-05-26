package com.jellystudy.common.web;

import com.jellystudy.common.health.HealthProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一健康检查：GET /api/health（含 mysql/redis/mongodb/rabbitmq 组件探活）
 */
@RestController
@RequestMapping("/api")
public class JellystudyHealthController {

    private final String serviceName;
    private final List<HealthProbe> probes;

    public JellystudyHealthController(
            @Value("${spring.application.name:unknown}") String serviceName,
            List<HealthProbe> probes) {
        this.serviceName = serviceName;
        this.probes = probes != null ? probes : Collections.emptyList();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, String> components = new LinkedHashMap<>();
        boolean allUp = true;

        List<HealthProbe> probeList = probes;
        if (!probeList.isEmpty()) {
            for (HealthProbe probe : probeList) {
                boolean up = probe.isUp();
                components.put(probe.component(), up ? "UP" : "DOWN");
                if (!up) {
                    allUp = false;
                }
            }
        }

        body.put("status", allUp ? "UP" : "DEGRADED");
        body.put("service", serviceName);
        if (!components.isEmpty()) {
            body.put("components", components);
        }
        body.put("timestamp", System.currentTimeMillis());
        return body;
    }
}
