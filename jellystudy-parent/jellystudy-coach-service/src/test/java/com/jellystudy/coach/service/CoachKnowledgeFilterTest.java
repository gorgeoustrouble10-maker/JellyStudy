package com.jellystudy.coach.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoachKnowledgeFilterTest {

    @Test
    void filtersDemoNames() {
        var out = CoachKnowledgeFilter.filterDemoNames(List.of("操作系统", "1+1=?", "Java 基础"));
        assertEquals(2, out.size());
        assertFalse(out.contains("1+1=?"));
    }

    @Test
    void detectsStaleMarkers() {
        assertTrue(CoachKnowledgeFilter.hasStaleCoachMarkers(List.of("Spring Boot"), "ok"));
        assertTrue(CoachKnowledgeFilter.hasStaleCoachMarkers(List.of(), "欢迎加入 JellyCoach"));
    }
}
