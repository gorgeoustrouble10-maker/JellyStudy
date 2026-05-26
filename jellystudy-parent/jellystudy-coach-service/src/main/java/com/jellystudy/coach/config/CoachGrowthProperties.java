package com.jellystudy.coach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "coach.growth")
public class CoachGrowthProperties {

    private int dailyGoalCount = 3;
    private int pointsPerTask = 10;
    private int pointsHighScore = 5;
    private int streakBonus = 50;
}
