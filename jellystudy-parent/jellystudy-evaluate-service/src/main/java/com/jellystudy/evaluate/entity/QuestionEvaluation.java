package com.jellystudy.evaluate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 问题评估记录实体
 */
@Entity
@Table(name = "question_evaluation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionEvaluation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "question_id", length = 36, nullable = false)
    private String questionId;

    @Column(name = "question_title", length = 500)
    private String questionTitle;

    @Column(name = "question_content", columnDefinition = "TEXT")
    private String questionContent;

    @Column(name = "knowledge_points", columnDefinition = "TEXT")
    private String knowledgePoints;

    @Column(length = 20)
    private String difficulty;

    @Column(name = "difficulty_description", length = 500)
    private String difficultyDescription;

    @Column(name = "evaluation_details", columnDefinition = "TEXT")
    private String evaluationDetails;

    @Column(name = "created_at")
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}
