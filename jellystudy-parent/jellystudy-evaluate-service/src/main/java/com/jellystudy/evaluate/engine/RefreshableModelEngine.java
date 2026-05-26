package com.jellystudy.evaluate.engine;

import com.jellystudy.common.entity.AnswerEvaluationDTO;
import com.jellystudy.common.entity.QuestionEvaluationDTO;
import com.jellystudy.evaluate.config.EvaluateModelProperties;
import com.jellystudy.evaluate.config.ModelEngineSelectorConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 每次评估时按最新 Nacos 配置选择引擎（type / api-key 变更后下一次调用生效）。
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RefreshableModelEngine implements ModelEngine {

    private final EvaluateModelProperties modelProperties;
    private final MockModelEngine mockEngine;
    private final QianWenModelEngine qianwenEngine;

    @Override
    public QuestionEvaluationDTO evaluateQuestion(String questionId, String title, String content) {
        return resolve().evaluateQuestion(questionId, title, content);
    }

    @Override
    public AnswerEvaluationDTO evaluateAnswer(String answerId, String questionId,
                                              String questionContent, String answerContent) {
        return resolve().evaluateAnswer(answerId, questionId, questionContent, answerContent);
    }

    private ModelEngine resolve() {
        if ("mock".equalsIgnoreCase(modelProperties.getType())) {
            return mockEngine;
        }
        if (ModelEngineSelectorConfiguration.hasValidDashScopeKey(modelProperties.getApiKey())) {
            return qianwenEngine;
        }
        log.debug("无有效 DashScope Key，评估走 Mock");
        return mockEngine;
    }
}
