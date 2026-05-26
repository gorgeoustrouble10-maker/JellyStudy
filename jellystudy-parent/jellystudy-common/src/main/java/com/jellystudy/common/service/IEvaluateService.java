package com.jellystudy.common.service;

import com.jellystudy.common.entity.AnswerEvaluationDTO;
import com.jellystudy.common.entity.QuestionEvaluationDTO;

/**
 * 评估服务Dubbo接口
 * 基于大模型实现问答评估功能
 */
public interface IEvaluateService {

    /**
     * 评估问题
     * - 提取问题知识点
     * - 进行难度分级（EASY/MEDIUM/HARD）
     * 
     * @param questionId 问题ID
     * @param questionTitle 问题标题
     * @param questionContent 问题内容
     * @return 问题评估结果
     */
    QuestionEvaluationDTO evaluateQuestion(String questionId, String questionTitle, String questionContent);

    /**
     * 评估答案（四参数兼容旧调用方）
     */
    default AnswerEvaluationDTO evaluateAnswer(String answerId, String questionId,
                                               String questionContent, String answerContent) {
        return evaluateAnswer(answerId, questionId, questionContent, answerContent, null);
    }

    /**
     * 评估答案；userId 为回答作者，用于 Coach MQ 积分归因
     */
    AnswerEvaluationDTO evaluateAnswer(String answerId, String questionId,
                                       String questionContent, String answerContent, String userId);

    /**
     * 删除某问题下的全部评估记录（问题评估 + 该问题下所有答案评估）
     */
    void deleteEvaluationsForQuestion(String questionId);

    /**
     * 删除某条答案的评估记录
     */
    void deleteEvaluationForAnswer(String answerId);
}
