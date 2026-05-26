package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCompletedEvent implements Serializable {

    private String answerId;
    private String questionId;
    private String questionContent;
    private String answerContent;
    private int score;
    private String grade;
    private String userId;
}
