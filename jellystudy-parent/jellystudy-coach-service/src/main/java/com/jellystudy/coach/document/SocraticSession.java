package com.jellystudy.coach.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "socratic_sessions")
public class SocraticSession {

    @Id
    private String id;
    private String userId;
    private String topic;
    private List<String> keyPoints;
    private List<String> masteredAspects;
    private List<String> misconceptions;
    private String logicChainComment;
    private List<String> recommendedPractice;
    private String summaryMarkdown;
    private Integer evaluateScore;
    private String evaluateGrade;
    private String evaluateDetails;
    private Date createdAt;
}
