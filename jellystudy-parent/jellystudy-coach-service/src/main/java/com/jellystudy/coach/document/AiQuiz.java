package com.jellystudy.coach.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ai_quizzes")
public class AiQuiz {

    @Id
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
