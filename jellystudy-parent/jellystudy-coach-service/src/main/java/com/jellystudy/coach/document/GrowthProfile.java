package com.jellystudy.coach.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "growth_profiles")
public class GrowthProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;
    private int totalPoints;
    private int streakDays;
    private List<String> weakPoints;
    private String lastDiagnosis;
    private List<String> learnedKnowledgePoints;
    private Date updatedAt;
}
