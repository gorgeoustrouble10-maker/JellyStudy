package com.jellystudy.qa.service;

import com.jellystudy.common.service.IEvaluateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步调用评估服务（独立 Bean，避免 @Async 自调用失效）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluateAsyncExecutor {

    @DubboReference(version = "1.0.0", protocol = "tri", check = false, timeout = 60000)
    private IEvaluateService evaluateService;

    @Async
    public void evaluateQuestionAsync(String questionId, String title, String content) {
        try {
            log.info("异步评估问题, questionId: {}", questionId);
            evaluateService.evaluateQuestion(questionId, title, content);
        } catch (Exception e) {
            log.warn("异步评估问题失败, questionId={}, {}", questionId, e.getMessage());
        }
    }

    @Async
    public void evaluateAnswerAsync(String answerId, String questionId, String questionContent,
                                    String answerContent, String author) {
        try {
            log.info("异步评估答案, answerId: {}, author: {}", answerId, author);
            evaluateService.evaluateAnswer(answerId, questionId, questionContent, answerContent, author);
        } catch (Exception e) {
            log.warn("异步评估答案失败, answerId={}, {}", answerId, e.getMessage());
        }
    }
}
