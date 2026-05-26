package com.jellystudy.coach.service;

import com.jellystudy.common.entity.AnswerEvaluationDTO;
import com.jellystudy.common.service.IEvaluateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class EvaluateBridgeService {

    @DubboReference(version = "1.0.0", protocol = "tri", check = false, timeout = 60000)
    private IEvaluateService evaluateService;

    /**
     * 将苏格拉底对话 transcript 交给评估服务 Dubbo 打分（P2 联动）
     */
    public AnswerEvaluationDTO evaluateSocraticDialogue(String topic, String transcript) {
        try {
            String questionContent = """
                【苏格拉底对话学情评估】
                话题：%s
                
                请根据以下完整对话，评估学习者对话题的理解程度、逻辑链完整性，以及是否存在概念误区。
                对话记录：
                %s
                """.formatted(topic, transcript);

            return evaluateService.evaluateAnswer(
                    UUID.randomUUID().toString(),
                    "socratic-" + Math.abs(topic.hashCode()),
                    questionContent,
                    "（评估对象：上述对话中学员的所有发言与推理过程，请给出100分制评分）");
        } catch (Exception e) {
            log.warn("Dubbo 调用评估服务失败，苏格拉底总结将仅使用 Coach AI", e);
            return null;
        }
    }
}
