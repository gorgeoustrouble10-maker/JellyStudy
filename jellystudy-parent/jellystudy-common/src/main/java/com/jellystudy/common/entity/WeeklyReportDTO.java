package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportDTO implements Serializable {

    private String userId;
    private int totalPoints;
    private int streakDays;
    /** 本周参考目标分（Nacos dailyGoal × 7 × pointsPerTask） */
    private int weeklyGoalPoints;
    /** 最近7天真实打卡（末位=今天） */
    private List<Boolean> recent7DayCheckIns;
    private List<Boolean> recent30DayCheckIns;
    private boolean checkedInToday;
    private List<String> learnedKnowledgePoints;
    private List<String> weakPoints;
    private List<SocraticSessionBriefDTO> recentSocraticSessions;
    private List<KnowledgeMasteryDTO> knowledgeMastery;
    private PointsTrendDTO pointsTrend;
    private String aiSummary;
    private String generatedAt;
}
