package com.jellystudy.qa.service;

import com.jellystudy.common.service.IEvaluateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 删除问题/答案时，通过 Dubbo 同步清理评估库记录
 */
@Slf4j
@Component
public class EvaluateCleanupClient {

    @DubboReference(version = "1.0.0", protocol = "tri", check = false, timeout = 5000)
    private IEvaluateService evaluateService;

    public void onQuestionDeleted(String questionId) {
        try {
            evaluateService.deleteEvaluationsForQuestion(questionId);
        } catch (Exception e) {
            log.warn("删除问题评估失败, questionId={}, {}", questionId, e.getMessage());
        }
    }

    public void onAnswerDeleted(String answerId) {
        try {
            evaluateService.deleteEvaluationForAnswer(answerId);
        } catch (Exception e) {
            log.warn("删除答案评估失败, answerId={}, {}", answerId, e.getMessage());
        }
    }
}
