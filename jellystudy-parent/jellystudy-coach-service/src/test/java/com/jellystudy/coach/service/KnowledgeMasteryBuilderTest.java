package com.jellystudy.coach.service;

import com.jellystudy.coach.document.AiQuiz;
import com.jellystudy.coach.document.SocraticSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeMasteryBuilderTest {

    private final KnowledgeMasteryBuilder builder = new KnowledgeMasteryBuilder();

    @Test
    void emptyLearnedReturnsEmpty() {
        assertTrue(builder.build(List.of(), List.of(), List.of(), List.of()).isEmpty());
    }

    @Test
    void quizAverageUsedWhenOnlyQuizzes() {
        AiQuiz q1 = new AiQuiz();
        q1.setWeakPoint("Redis");
        q1.setScore(80);
        AiQuiz q2 = new AiQuiz();
        q2.setWeakPoint("redis");
        q2.setScore(60);
        var result = builder.build(List.of("Redis"), List.of(), List.of(q1, q2), List.of());
        assertEquals(70, result.get(0).getPercent());
        assertEquals("quiz", result.get(0).getSource());
    }

    @Test
    void weakPointWithoutPracticeUsesDiagnosis() {
        var result = builder.build(List.of("Dubbo"), List.of("Dubbo"), List.of(), List.of());
        assertEquals(45, result.get(0).getPercent());
        assertEquals("diagnosis", result.get(0).getSource());
        assertEquals("待测评", result.get(0).getStatus());
    }

    @Test
    void mixedQuizAndSocraticAveragesScores() {
        AiQuiz q = new AiQuiz();
        q.setWeakPoint("MQ");
        q.setScore(80);
        SocraticSession s = new SocraticSession();
        s.setTopic("MQ");
        s.setEvaluateScore(60);
        var result = builder.build(List.of("MQ"), List.of(), List.of(q), List.of(s));
        assertEquals(70, result.get(0).getPercent());
        assertEquals("mixed", result.get(0).getSource());
    }
}
