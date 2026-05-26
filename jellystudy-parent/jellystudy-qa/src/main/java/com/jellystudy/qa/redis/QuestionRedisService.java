package com.jellystudy.qa.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.common.entity.QuestionDTO;
import com.jellystudy.qa.config.JellystudyRedisProperties;
import com.jellystudy.qa.entity.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Redis：热门榜(ZSET) + 浏览排行(ZSET) + 问题详情缓存(HASH/String)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionRedisService {

    private final StringRedisTemplate redisTemplate;
    private final JellystudyRedisProperties properties;
    private final ObjectMapper objectMapper;

    public void onQuestionCreated(Question question) {
        syncQuestionToRedis(question);
    }

    /** 编辑问题后：刷新 ZSET 分数（updatedAt 影响衰减）并更新详情缓存 */
    public void onQuestionUpdated(Question question) {
        syncQuestionToRedis(question);
    }

    private void syncQuestionToRedis(Question question) {
        if (question == null || question.getId() == null) {
            return;
        }
        upsertRankings(question);
        cacheQuestion(question);
    }

    public void onQuestionViewed(Question question) {
        if (question == null || question.getId() == null) {
            return;
        }
        upsertRankings(question);
        cacheQuestion(question);
    }

    public void onLikeIncremented(Question question) {
        if (question == null || question.getId() == null) {
            return;
        }
        upsertRankings(question);
        cacheQuestion(question);
    }

    public void onAnswerCountIncremented(Question question) {
        if (question == null || question.getId() == null) {
            return;
        }
        upsertRankings(question);
        cacheQuestion(question);
    }

    public void evictQuestion(String questionId) {
        if (questionId == null) {
            return;
        }
        redisTemplate.delete(cacheKey(questionId));
        redisTemplate.opsForZSet().remove(properties.getHotKey(), questionId);
        redisTemplate.opsForZSet().remove(properties.getViewRankKey(), questionId);
    }

    public QuestionDTO getCachedQuestion(String questionId) {
        String json = redisTemplate.opsForValue().get(cacheKey(questionId));
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, QuestionDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("反序列化问题缓存失败, id={}", questionId, e);
            return null;
        }
    }

    public List<QuestionDTO> getHotTop(int limit, Function<String, QuestionDTO> loader) {
        return loadRanked(properties.getHotKey(), limit, loader);
    }

    public List<QuestionDTO> getMostViewedTop(int limit, Function<String, QuestionDTO> loader) {
        return loadRanked(properties.getViewRankKey(), limit, loader);
    }

    public void rebuildRankings(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            redisTemplate.delete(properties.getHotKey());
            redisTemplate.delete(properties.getViewRankKey());
            return;
        }
        redisTemplate.delete(properties.getHotKey());
        redisTemplate.delete(properties.getViewRankKey());
        int synced = 0;
        for (Question q : questions) {
            if (q.getId() == null) {
                continue;
            }
            upsertRankings(q);
            synced++;
        }
        log.info("已从 MySQL 全量同步 {} 条问题到 Redis 排行榜（最近 {} 天窗口）",
                synced, properties.getRecentWindowDays());
    }

    public boolean isRankingEmpty() {
        Long hot = redisTemplate.opsForZSet().zCard(properties.getHotKey());
        return hot == null || hot == 0;
    }

    private void upsertRankings(Question question) {
        int days = properties.getRecentWindowDays();
        double hot = QuestionRankScoring.computeHotScore(question, days);
        double view = QuestionRankScoring.computeViewRankScore(question, days);
        if (hot <= 0) {
            redisTemplate.opsForZSet().remove(properties.getHotKey(), question.getId());
        } else {
            redisTemplate.opsForZSet().add(properties.getHotKey(), question.getId(), hot);
        }
        if (view <= 0) {
            redisTemplate.opsForZSet().remove(properties.getViewRankKey(), question.getId());
        } else {
            redisTemplate.opsForZSet().add(properties.getViewRankKey(), question.getId(), view);
        }
    }

    private List<QuestionDTO> loadRanked(String zsetKey, int limit,
                                         Function<String, QuestionDTO> loader) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(zsetKey, 0, limit - 1);
        if (tuples == null || tuples.isEmpty()) {
            return new ArrayList<>();
        }
        List<QuestionDTO> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null) {
                continue;
            }
            QuestionDTO cached = getCachedQuestion(tuple.getValue());
            if (cached != null) {
                result.add(cached);
                continue;
            }
            QuestionDTO loaded = loader.apply(tuple.getValue());
            if (loaded != null) {
                result.add(loaded);
            }
        }
        return result.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void cacheQuestion(Question question) {
        try {
            QuestionDTO dto = toDto(question);
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(
                    cacheKey(question.getId()),
                    json,
                    Duration.ofMinutes(properties.getQuestionCacheTtlMinutes()));
        } catch (JsonProcessingException e) {
            log.warn("缓存问题详情失败, id={}", question.getId(), e);
        }
    }

    private QuestionDTO toDto(Question entity) {
        return QuestionDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .knowledgePointId(entity.getKnowledgePointId())
                .author(entity.getAuthor())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .answerCount(entity.getAnswerCount())
                .build();
    }

    private String cacheKey(String questionId) {
        return properties.getQuestionCachePrefix() + questionId;
    }

    public double computeHotScore(Question q) {
        return QuestionRankScoring.computeHotScore(q, properties.getRecentWindowDays());
    }
}
