package com.jellystudy.coach.redis;

import lombok.Builder;

@Builder
public record DailyCheckInResult(
        int consecutiveDays,
        boolean checkedInToday,
        boolean newlyCheckedIn,
        String message
) {}
