package com.jellystudy.evaluate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 答案评估记录实体
 */
@Entity
@Table(name = "answer_evaluation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerEvaluation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "answer_id", length = 36, nullable = false)
    private String answerId;

    @Column(name = "question_id", length = 36)
    private String questionId;

    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;

    @Column(nullable = false)
    private Integer score;

    @Column(length = 10)
    private String grade;

    @Column(name = "evaluation_details", columnDefinition = "TEXT")
    private String evaluationDetails;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "reference_answer", columnDefinition = "TEXT")
    private String referenceAnswer;

    @Column(name = "created_at")
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}
