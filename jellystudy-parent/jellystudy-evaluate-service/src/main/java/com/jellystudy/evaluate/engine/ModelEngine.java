package com.jellystudy.evaluate.engine;

import com.jellystudy.common.entity.AnswerEvaluationDTO;
import com.jellystudy.common.entity.QuestionEvaluationDTO;

/**
 * 大模型引擎接口
 * 定义问题评估和答案评估的标准接口
 */
public interface ModelEngine {

    /**
     * 评估问题
     * - 提取问题知识点
     * - 进行难度分级
     *
     * @param questionId 问题ID
     * @param title 问题标题
     * @param content 问题内容
     * @return 问题评估结果
     */
    QuestionEvaluationDTO evaluateQuestion(String questionId, String title, String content);

    /**
     * 评估答案
     * - 对用户答案进行打分
     * - 提供评分详情和改进建议
     *
     * @param answerId 答案ID
     * @param questionId 问题ID
     * @param questionContent 问题内容
     * @param answerContent 用户答案内容
     * @return 答案评估结果
     */
    AnswerEvaluationDTO evaluateAnswer(String answerId, String questionId, String questionContent, String answerContent);
}
