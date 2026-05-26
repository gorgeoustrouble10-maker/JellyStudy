package com.jellystudy.evaluate.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelEngineSelectorConfigurationTest {

    @Test
    void acceptsRealSkKey() {
        assertTrue(ModelEngineSelectorConfiguration.hasValidDashScopeKey("sk-abc1234567890"));
    }

    @Test
    void rejectsPlaceholder() {
        assertFalse(ModelEngineSelectorConfiguration.hasValidDashScopeKey("sk-请替换为你的通义千问密钥"));
    }

    @Test
    void rejectsEmpty() {
        assertFalse(ModelEngineSelectorConfiguration.hasValidDashScopeKey(""));
    }
}
