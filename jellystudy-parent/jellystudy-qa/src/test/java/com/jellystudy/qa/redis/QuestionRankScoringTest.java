package com.jellystudy.qa.redis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 热门榜得分公式单测（与 Redis ZSET 写入逻辑一致）
 */
class QuestionRankScoringTest {

    static double hotScore(long views, long answers) {
        return views * 1.0 + answers * 3.0;
    }

    @Test
    void hotScoreWeightsAnswersHigher() {
        assertEquals(10.0, hotScore(10, 0));
        assertEquals(16.0, hotScore(10, 2));
    }

    @Test
    void hotScoreZeroWhenNoActivity() {
        assertEquals(0.0, hotScore(0, 0));
    }
}
