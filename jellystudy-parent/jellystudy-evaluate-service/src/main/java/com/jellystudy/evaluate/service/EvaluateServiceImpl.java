package com.jellystudy.evaluate.service;

import com.jellystudy.common.entity.AnswerEvaluationDTO;
import com.jellystudy.common.entity.QuestionEvaluationDTO;
import com.jellystudy.common.service.IEvaluateService;
import com.jellystudy.evaluate.engine.ModelEngine;
import com.jellystudy.evaluate.entity.AnswerEvaluation;
import com.jellystudy.evaluate.entity.QuestionEvaluation;
import com.jellystudy.evaluate.redis.EvaluationRedisCache;
import com.jellystudy.evaluate.mq.EvaluationEventPublisher;
import com.jellystudy.evaluate.repository.AnswerEvaluationRepository;
import com.jellystudy.evaluate.repository.QuestionEvaluationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 评估服务Dubbo实现
 * 基于大模型实现问答评估功能
 */
@Slf4j
@Service
@DubboService(version = "1.0.0", protocol = "tri")
public class EvaluateServiceImpl implements IEvaluateService {

    private final ModelEngine modelEngine;
    private final QuestionEvaluationRepository questionEvaluationRepository;
    private final AnswerEvaluationRepository answerEvaluationRepository;
    private final EvaluationRedisCache evaluationRedisCache;
    private final EvaluationEventPublisher evaluationEventPublisher;
    private final ObjectMapper objectMapper;

    public EvaluateServiceImpl(ModelEngine modelEngine,
                              QuestionEvaluationRepository questionEvaluationRepository,
                              AnswerEvaluationRepository answerEvaluationRepository,
                              EvaluationRedisCache evaluationRedisCache,
                              EvaluationEventPublisher evaluationEventPublisher,
                              ObjectMapper objectMapper) {
        this.modelEngine = modelEngine;
        this.questionEvaluationRepository = questionEvaluationRepository;
        this.answerEvaluationRepository = answerEvaluationRepository;
        this.evaluationRedisCache = evaluationRedisCache;
        this.evaluationEventPublisher = evaluationEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @SuppressWarnings({"null", "unused"})
    public QuestionEvaluationDTO evaluateQuestion(String questionId, String questionTitle, String questionContent) {
        log.info("收到问题评估请求, questionId: {}, title: {}", questionId, questionTitle);
        
        try {
            // 调用大模型引擎评估问题
            QuestionEvaluationDTO result = modelEngine.evaluateQuestion(questionId, questionTitle, questionContent);
            
            // 检查结果是否为null
            if (result == null) {
                log.warn("模型引擎返回null结果, 使用降级策略");
                return createFallbackQuestionEvaluation(questionId, questionTitle, questionContent);
            }
            
            QuestionEvaluation entity = questionEvaluationRepository.findByQuestionId(questionId)
                    .map(existing -> {
                        existing.setQuestionTitle(questionTitle);
                        existing.setQuestionContent(questionContent);
                        existing.setKnowledgePoints(toJson(result.getKnowledgePoints()));
                        existing.setDifficulty(result.getDifficulty());
                        existing.setDifficultyDescription(result.getDifficultyDescription());
                        existing.setEvaluationDetails(result.getEvaluationDetails());
                        return existing;
                    })
                    .orElseGet(() -> QuestionEvaluation.builder()
                            .id(result.getId() != null ? result.getId() : UUID.randomUUID().toString())
                            .questionId(questionId)
                            .questionTitle(questionTitle)
                            .questionContent(questionContent)
                            .knowledgePoints(toJson(result.getKnowledgePoints()))
                            .difficulty(result.getDifficulty())
                            .difficultyDescription(result.getDifficultyDescription())
                            .evaluationDetails(result.getEvaluationDetails())
                            .createdAt(result.getCreatedAt() != null ? result.getCreatedAt() : new Date())
                            .build());

            questionEvaluationRepository.save(entity);
            evaluationRedisCache.putQuestionEvaluation(questionId, entity);
            log.info("问题评估完成并保存, questionId: {}, difficulty: {}", questionId, result.getDifficulty());
            
            return result;
            
        } catch (Exception e) {
            log.error("问题评估失败, questionId: {}", questionId, e);
            // 返回降级结果
            return createFallbackQuestionEvaluation(questionId, questionTitle, questionContent);
        }
    }

    @Override
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @SuppressWarnings({"null", "unused"})
    public AnswerEvaluationDTO evaluateAnswer(String answerId, String questionId,
                                              String questionContent, String answerContent) {
        return evaluateAnswer(answerId, questionId, questionContent, answerContent, null);
    }

    @Override
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @SuppressWarnings({"null", "unused"})
    public AnswerEvaluationDTO evaluateAnswer(String answerId, String questionId,
                                              String questionContent, String answerContent, String userId) {
        log.info("收到答案评估请求, answerId: {}, userId: {}", answerId, userId);

        try {
            AnswerEvaluationDTO result = modelEngine.evaluateAnswer(answerId, questionId, questionContent, answerContent);

            if (result == null) {
                log.warn("模型引擎返回null结果, 使用降级策略");
                return createFallbackAnswerEvaluation(answerId, questionId, answerContent);
            }

            AnswerEvaluation entity = answerEvaluationRepository.findByAnswerId(answerId)
                    .map(existing -> {
                        existing.setQuestionId(questionId);
                        existing.setAnswerContent(answerContent);
                        existing.setScore(result.getScore());
                        existing.setGrade(result.getGrade());
                        existing.setEvaluationDetails(result.getEvaluationDetails());
                        existing.setStrengths(toJson(result.getStrengths()));
                        existing.setSuggestions(toJson(result.getSuggestions()));
                        existing.setReferenceAnswer(result.getReferenceAnswer());
                        return existing;
                    })
                    .orElseGet(() -> AnswerEvaluation.builder()
                            .id(result.getId() != null ? result.getId() : UUID.randomUUID().toString())
                            .answerId(answerId)
                            .questionId(questionId)
                            .answerContent(answerContent)
                            .score(result.getScore())
                            .grade(result.getGrade())
                            .evaluationDetails(result.getEvaluationDetails())
                            .strengths(toJson(result.getStrengths()))
                            .suggestions(toJson(result.getSuggestions()))
                            .referenceAnswer(result.getReferenceAnswer())
                            .createdAt(result.getCreatedAt() != null ? result.getCreatedAt() : new Date())
                            .build());

            answerEvaluationRepository.save(entity);
            evaluationRedisCache.putAnswerEvaluation(answerId, entity);
            evaluationEventPublisher.publishAnswerEvaluated(result, questionContent, answerContent, userId);
            log.info("答案评估完成并保存, answerId: {}, score: {}", answerId, result.getScore());

            return result;

        } catch (Exception e) {
            log.error("答案评估失败, answerId: {}", answerId, e);
            return createFallbackAnswerEvaluation(answerId, questionId, answerContent);
        }
    }

    /**
     * 创建降级问题评估结果
     */
    private QuestionEvaluationDTO createFallbackQuestionEvaluation(String questionId, 
                                                                   String questionTitle, 
                                                                   String questionContent) {
        log.warn("使用降级策略处理问题评估, questionId: {}", questionId);
        return QuestionEvaluationDTO.builder()
                .id(UUID.randomUUID().toString())
                .questionId(questionId)
                .questionTitle(questionTitle)
                .questionContent(questionContent)
                .knowledgePoints(List.of("未知知识点"))
                .difficulty("MEDIUM")
                .difficultyDescription("中等：需要一定的知识积累和分析能力")
                .evaluationDetails("【问题评估报告】\n评估服务暂时不可用，使用默认评估结果")
                .build();
    }

    /**
     * 创建降级答案评估结果
     */
    private AnswerEvaluationDTO createFallbackAnswerEvaluation(String answerId, 
                                                               String questionId, 
                                                               String answerContent) {
        log.warn("使用降级策略处理答案评估, answerId: {}", answerId);
        return AnswerEvaluationDTO.builder()
                .id(UUID.randomUUID().toString())
                .answerId(answerId)
                .questionId(questionId)
                .answerContent(answerContent)
                .score(60)
                .grade("C")
                .evaluationDetails("【答案评估报告】\n评估服务暂时不可用，使用默认评分")
                .strengths(List.of("回答符合基本要求"))
                .suggestions(List.of("建议在评估服务恢复后重新评估"))
                .referenceAnswer("评估服务暂时不可用，无法生成参考答案")
                .build();
    }

    @Override
    @Transactional
    public void deleteEvaluationsForQuestion(String questionId) {
        if (questionId == null || questionId.isBlank()) {
            return;
        }
        List<AnswerEvaluation> answerEvals = answerEvaluationRepository.findByQuestionId(questionId);
        for (AnswerEvaluation evaluation : answerEvals) {
            if (evaluation.getAnswerId() != null) {
                evaluationRedisCache.invalidateAnswer(evaluation.getAnswerId());
            }
        }
        answerEvaluationRepository.deleteByQuestionId(questionId);
        questionEvaluationRepository.deleteByQuestionId(questionId);
        evaluationRedisCache.invalidateQuestion(questionId);
        log.info("已删除问题关联评估, questionId={}", questionId);
    }

    @Override
    @Transactional
    public void deleteEvaluationForAnswer(String answerId) {
        if (answerId == null || answerId.isBlank()) {
            return;
        }
        answerEvaluationRepository.deleteByAnswerId(answerId);
        evaluationRedisCache.invalidateAnswer(answerId);
        log.info("已删除答案评估, answerId={}", answerId);
    }

    /**
     * 将列表转换为JSON字符串
     */
    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "[]";
        }
    }
}
