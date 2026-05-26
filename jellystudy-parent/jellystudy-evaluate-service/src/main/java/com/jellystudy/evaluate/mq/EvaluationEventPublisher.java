package com.jellystudy.evaluate.mq;

import com.jellystudy.common.entity.EvaluationCompletedEvent;
import com.jellystudy.common.entity.AnswerEvaluationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${coach.rabbit.evaluation-queue}")
    private String evaluationQueue;

    public void publishAnswerEvaluated(AnswerEvaluationDTO result, String questionContent, String answerContent,
                                       String userId) {
        try {
            String resolvedUser = (userId != null && !userId.isBlank()) ? userId.trim() : "demo-user";
            EvaluationCompletedEvent event = EvaluationCompletedEvent.builder()
                    .answerId(result.getAnswerId())
                    .questionId(result.getQuestionId())
                    .questionContent(questionContent)
                    .answerContent(answerContent)
                    .score(result.getScore())
                    .grade(result.getGrade())
                    .userId(resolvedUser)
                    .build();
            rabbitTemplate.convertAndSend(evaluationQueue, event);
            log.info("已发布评估完成事件 answerId={} score={}", result.getAnswerId(), result.getScore());
        } catch (Exception e) {
            log.warn("RabbitMQ 不可用，跳过评估事件发布（Coach 模块需启动 RabbitMQ）", e);
        }
    }
}
