package com.jellystudy.coach.service;

import com.jellystudy.coach.document.AiQuiz;
import com.jellystudy.coach.document.SocraticSession;
import com.jellystudy.common.entity.KnowledgeMasteryDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class KnowledgeMasteryBuilder {

    public List<KnowledgeMasteryDTO> build(List<String> learned,
                                           List<String> weakPoints,
                                           List<AiQuiz> quizzes,
                                           List<SocraticSession> socraticSessions) {
        if (learned == null || learned.isEmpty()) {
            return List.of();
        }
        Map<String, List<Integer>> quizScores = new HashMap<>();
        if (quizzes != null) {
            for (AiQuiz q : quizzes) {
                if (q.getWeakPoint() == null || q.getScore() == null) {
                    continue;
                }
                quizScores.computeIfAbsent(normalize(q.getWeakPoint()), k -> new ArrayList<>())
                        .add(q.getScore());
            }
        }
        Map<String, Integer> socraticScores = new HashMap<>();
        if (socraticSessions != null) {
            for (SocraticSession s : socraticSessions) {
                if (s.getTopic() == null || s.getEvaluateScore() == null) {
                    continue;
                }
                socraticScores.put(normalize(s.getTopic()), s.getEvaluateScore());
            }
        }
        List<String> weak = weakPoints != null ? weakPoints : List.of();
        List<KnowledgeMasteryDTO> result = new ArrayList<>();
        for (String name : learned) {
            String key = normalize(name);
            List<Integer> qs = quizScores.get(key);
            Integer ss = socraticScores.get(key);
            int percent;
            String source;
            if (qs != null && !qs.isEmpty() && ss != null) {
                int quizAvg = (int) qs.stream().mapToInt(Integer::intValue).average().orElse(0);
                percent = (quizAvg + ss) / 2;
                source = "mixed";
            } else if (qs != null && !qs.isEmpty()) {
                percent = (int) qs.stream().mapToInt(Integer::intValue).average().orElse(0);
                source = "quiz";
            } else if (ss != null) {
                percent = ss;
                source = "socratic";
            } else if (weak.stream().anyMatch(w -> matches(name, w))) {
                percent = 45;
                source = "diagnosis";
            } else {
                percent = 0;
                source = "none";
            }
            result.add(KnowledgeMasteryDTO.builder()
                    .name(name)
                    .percent(Math.min(100, Math.max(0, percent)))
                    .status(statusFor(percent, source))
                    .source(source)
                    .build());
        }
        return result;
    }

    private static String statusFor(int percent, String source) {
        if ("none".equals(source)) {
            return "暂无练习";
        }
        if ("diagnosis".equals(source)) {
            return "待测评";
        }
        if (percent >= 80) {
            return "已掌握";
        }
        if (percent >= 60) {
            return "待巩固";
        }
        return "薄弱";
    }

    static boolean matches(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
