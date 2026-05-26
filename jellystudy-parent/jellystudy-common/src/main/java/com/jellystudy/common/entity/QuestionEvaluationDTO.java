package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 问题评估DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionEvaluationDTO {

    /**
     * 评估记录ID
     */
    private String id;

    /**
     * 问题ID
     */
    private String questionId;

    /**
     * 问题标题
     */
    private String questionTitle;

    /**
     * 问题内容
     */
    private String questionContent;

    /**
     * 提取的知识点列表
     */
    private List<String> knowledgePoints;

    /**
     * 难度等级：EASY(简单), MEDIUM(中等), HARD(困难)
     */
    private String difficulty;

    /**
     * 难度描述
     */
    private String difficultyDescription;

    /**
     * 评估详情
     */
    private String evaluationDetails;

    /**
     * 创建时间
     */
    private Date createdAt;
}
