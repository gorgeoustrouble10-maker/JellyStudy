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
@Document(collection = "weekly_snapshots")
public class WeeklySnapshot {

    @Id
    private String id;
    private String userId;
    /** 如 2026-W21 */
    private String weekKey;
    private int totalPoints;
    private int streakDays;
    private Date createdAt;
}
