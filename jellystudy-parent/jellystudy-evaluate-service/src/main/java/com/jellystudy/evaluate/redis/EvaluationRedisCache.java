package com.jellystudy.evaluate.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.evaluate.config.EvaluateRedisProperties;
import com.jellystudy.evaluate.entity.AnswerEvaluation;
import com.jellystudy.evaluate.entity.QuestionEvaluation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 自设计 Redis 场景：评估结果读缓存，降低 MySQL 查询压力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationRedisCache {

    private final StringRedisTemplate redisTemplate;
    private final EvaluateRedisProperties properties;
    private final ObjectMapper objectMapper;

    public Optional<QuestionEvaluation> getQuestionEvaluation(String questionId,
                                                              Supplier<Optional<QuestionEvaluation>> dbLoader) {
        String key = properties.getQuestionEvalPrefix() + questionId;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null && !cached.isBlank()) {
            try {
                return Optional.of(objectMapper.readValue(cached, QuestionEvaluation.class));
            } catch (JsonProcessingException e) {
                log.warn("问题评估缓存反序列化失败, questionId={}", questionId, e);
            }
        }
        Optional<QuestionEvaluation> fromDb = dbLoader.get();
        fromDb.ifPresent(e -> putQuestionEvaluation(questionId, e));
        return fromDb;
    }

    public Optional<AnswerEvaluation> getAnswerEvaluation(String answerId,
                                                          Supplier<Optional<AnswerEvaluation>> dbLoader) {
        String key = properties.getAnswerEvalPrefix() + answerId;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null && !cached.isBlank()) {
            try {
                return Optional.of(objectMapper.readValue(cached, AnswerEvaluation.class));
            } catch (JsonProcessingException e) {
                log.warn("答案评估缓存反序列化失败, answerId={}", answerId, e);
            }
        }
        Optional<AnswerEvaluation> fromDb = dbLoader.get();
        fromDb.ifPresent(e -> putAnswerEvaluation(answerId, e));
        return fromDb;
    }

    public void putQuestionEvaluation(String questionId, QuestionEvaluation evaluation) {
        if (evaluation == null) {
            return;
        }
        write(properties.getQuestionEvalPrefix() + questionId, evaluation);
    }

    public void putAnswerEvaluation(String answerId, AnswerEvaluation evaluation) {
        if (evaluation == null) {
            return;
        }
        write(properties.getAnswerEvalPrefix() + answerId, evaluation);
    }

    public void invalidateQuestion(String questionId) {
        redisTemplate.delete(properties.getQuestionEvalPrefix() + questionId);
    }

    public void invalidateAnswer(String answerId) {
        redisTemplate.delete(properties.getAnswerEvalPrefix() + answerId);
    }

    public void cacheAllQuestionEvaluations(List<QuestionEvaluation> list) {
        if (list == null) {
            return;
        }
        for (QuestionEvaluation e : list) {
            if (e.getQuestionId() != null) {
                putQuestionEvaluation(e.getQuestionId(), e);
            }
        }
    }

    private void write(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(value),
                    Duration.ofMinutes(properties.getEvalCacheTtlMinutes()));
        } catch (JsonProcessingException e) {
            log.warn("写入评估缓存失败, key={}", key, e);
        }
    }
}
