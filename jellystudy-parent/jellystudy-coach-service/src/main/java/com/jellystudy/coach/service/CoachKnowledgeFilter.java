package com.jellystudy.coach.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 过滤课程演示用知识点，避免 Coach 白名单与薄弱点被 demo 数据主导。
 */
public final class CoachKnowledgeFilter {

    private CoachKnowledgeFilter() {
    }

    public static boolean isDemoKnowledge(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        String n = name.trim().toLowerCase(Locale.ROOT);
        return n.contains("1+1")
                || n.contains("demo")
                || n.equals("redis与缓存")
                || n.startsWith("test-");
    }

    public static List<String> filterDemoNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<String> filtered = names.stream()
                .filter(n -> !isDemoKnowledge(n))
                .collect(Collectors.toCollection(ArrayList::new));
        return filtered.isEmpty() ? new ArrayList<>(names) : filtered;
    }

    public static boolean hasStaleCoachMarkers(List<String> weakPoints, String diagnosis) {
        if (weakPoints == null || weakPoints.isEmpty()) {
            return true;
        }
        boolean weakStale = weakPoints.stream().anyMatch(w ->
                w != null && (w.contains("Spring Boot") || w.contains("微服务") || isDemoKnowledge(w)));
        boolean diagStale = diagnosis == null || diagnosis.isBlank()
                || diagnosis.contains("Spring Boot")
                || diagnosis.contains("已同步您的")
                || diagnosis.contains("欢迎加入 JellyCoach");
        return weakStale || diagStale;
    }
}
