package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiQuizDTO implements Serializable {

    private String id;
    private String userId;
    private String weakPoint;
    private String question;
    private String hint;
    private String userAnswer;
    private Integer score;
    private String feedback;
    private String status;
    private Date createdAt;
}
