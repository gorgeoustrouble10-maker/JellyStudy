package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 答案评估DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerEvaluationDTO {

    /**
     * 评估记录ID
     */
    private String id;

    /**
     * 答案ID
     */
    private String answerId;

    /**
     * 问题ID
     */
    private String questionId;

    /**
     * 用户答案内容
     */
    private String answerContent;

    /**
     * 得分（0-100）
     */
    private Integer score;

    /**
     * 评分等级：A(90-100), B(80-89), C(60-79), D(0-59)
     */
    private String grade;

    /**
     * 评分详情
     */
    private String evaluationDetails;

    /**
     * 优点
     */
    private List<String> strengths;

    /**
     * 改进建议
     */
    private List<String> suggestions;

    /**
     * 参考答案（可选）
     */
    private String referenceAnswer;

    /**
     * 创建时间
     */
    private Date createdAt;
}
