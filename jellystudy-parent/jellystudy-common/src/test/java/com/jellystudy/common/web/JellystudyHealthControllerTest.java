package com.jellystudy.common.web;

import com.jellystudy.common.health.HealthProbe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JellystudyHealthControllerTest {

    @Test
    void healthUpWhenAllProbesUp() {
        JellystudyHealthController controller = new JellystudyHealthController("test-service", List.of(
                probe("mysql", true),
                probe("redis", true)));
        Map<String, Object> body = controller.health();
        assertEquals("UP", body.get("status"));
        assertEquals("test-service", body.get("service"));
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) body.get("components");
        assertEquals("UP", components.get("mysql"));
        assertEquals("UP", components.get("redis"));
    }

    @Test
    void healthDegradedWhenProbeDown() {
        JellystudyHealthController controller = new JellystudyHealthController("coach", List.of(
                probe("dashscope", false)));
        Map<String, Object> body = controller.health();
        assertEquals("DEGRADED", body.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) body.get("components");
        assertEquals("DOWN", components.get("dashscope"));
    }

    @Test
    void healthUpWithNoProbes() {
        JellystudyHealthController controller = new JellystudyHealthController("gateway", List.of());
        Map<String, Object> body = controller.health();
        assertEquals("UP", body.get("status"));
        assertTrue(body.containsKey("timestamp"));
    }

    private static HealthProbe probe(String name, boolean up) {
        return new HealthProbe() {
            @Override
            public String component() {
                return name;
            }

            @Override
            public boolean isUp() {
                return up;
            }
        };
    }
}
