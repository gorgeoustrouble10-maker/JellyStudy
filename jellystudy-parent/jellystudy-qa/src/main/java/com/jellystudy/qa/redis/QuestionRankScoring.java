package com.jellystudy.qa.redis;

import com.jellystudy.qa.entity.Question;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 「最近」排行榜得分：基础热度 × 时间衰减，超出窗口返回 0（不入榜）
 */
public final class QuestionRankScoring {

    private QuestionRankScoring() {
    }

    public static double computeHotScore(Question q, int recentWindowDays) {
        if (q == null) {
            return 0;
        }
        double base = q.getLikeCount() * 3.0 + q.getAnswerCount() * 2.0 + q.getViewCount() * 0.1;
        double factor = recentFactor(q, recentWindowDays);
        return base * factor;
    }

    public static double computeViewRankScore(Question q, int recentWindowDays) {
        if (q == null) {
            return 0;
        }
        double factor = recentFactor(q, recentWindowDays);
        return q.getViewCount() * factor;
    }

    static double recentFactor(Question q, int recentWindowDays) {
        if (recentWindowDays <= 0) {
            return 1.0;
        }
        Date anchor = q.getUpdatedAt() != null ? q.getUpdatedAt() : q.getCreatedAt();
        if (anchor == null) {
            return 1.0;
        }
        long days = ChronoUnit.DAYS.between(anchor.toInstant(), Instant.now());
        if (days > recentWindowDays) {
            return 0;
        }
        return 1.0 - (days * 0.85 / recentWindowDays);
    }
}
