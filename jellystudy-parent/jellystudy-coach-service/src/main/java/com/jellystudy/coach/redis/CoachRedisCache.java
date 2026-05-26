package com.jellystudy.coach.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.common.entity.DailyTaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoachRedisCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${coach.redis.today-tasks-prefix}")
    private String todayTasksPrefix;

    @Value("${coach.redis.streak-prefix}")
    private String streakPrefix;

    @Value("${coach.redis.leaderboard-key}")
    private String leaderboardKey;

    public void cacheTodayTasks(String userId, List<DailyTaskDTO> tasks) {
        try {
            String key = todayTasksPrefix + userId + ":" + LocalDate.now();
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(tasks), Duration.ofHours(26));
        } catch (Exception e) {
            log.warn("缓存今日任务失败", e);
        }
    }

    public List<DailyTaskDTO> getTodayTasks(String userId) {
        try {
            String key = todayTasksPrefix + userId + ":" + LocalDate.now();
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("读取今日任务缓存失败", e);
            return null;
        }
    }

    public void invalidateTodayTasks(String userId) {
        String key = todayTasksPrefix + userId + ":" + LocalDate.now();
        redisTemplate.delete(key);
    }

    /**
     * 真实每日打卡：同一天内多次练习只计 1 次；必须跨自然日且连续才累加 streak。
     */
    public DailyCheckInResult recordDailyCheckIn(String userId) {
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();
        String lastKey = streakPrefix + userId + ":last";
        String countKey = streakPrefix + userId + ":consecutive";
        String datesKey = streakPrefix + userId + ":dates";

        String lastStr = redisTemplate.opsForValue().get(lastKey);
        int current = getStreak(userId);

        if (todayStr.equals(lastStr)) {
            return DailyCheckInResult.builder()
                    .consecutiveDays(current)
                    .checkedInToday(true)
                    .newlyCheckedIn(false)
                    .message("今日已打卡，连续 " + current + " 天")
                    .build();
        }

        int newStreak;
        if (lastStr == null || lastStr.isBlank()) {
            newStreak = 1;
        } else {
            LocalDate last = LocalDate.parse(lastStr);
            if (last.equals(today.minusDays(1))) {
                newStreak = current + 1;
            } else {
                newStreak = 1;
            }
        }

        redisTemplate.opsForValue().set(countKey, String.valueOf(newStreak));
        redisTemplate.opsForValue().set(lastKey, todayStr);
        redisTemplate.opsForSet().add(datesKey, todayStr);
        redisTemplate.expire(datesKey, 90, TimeUnit.DAYS);
        redisTemplate.delete(streakPrefix + userId);

        log.info("用户 {} 打卡成功，连续 {} 天（上次 {}）", userId, newStreak, lastStr);
        return DailyCheckInResult.builder()
                .consecutiveDays(newStreak)
                .checkedInToday(true)
                .newlyCheckedIn(true)
                .message("打卡成功！连续学习 " + newStreak + " 天")
                .build();
    }

    public int getStreak(String userId) {
        String val = redisTemplate.opsForValue().get(streakPrefix + userId + ":consecutive");
        return val == null ? 0 : Integer.parseInt(val);
    }

    public boolean hasCheckedInToday(String userId) {
        String lastStr = redisTemplate.opsForValue().get(streakPrefix + userId + ":last");
        return LocalDate.now().toString().equals(lastStr);
    }

    /** 最近 7 个自然日（含今天）是否打卡，索引 0=6天前 … 6=今天 */
    public List<Boolean> getRecent7DayCheckIns(String userId) {
        return getRecentDayCheckIns(userId, 7);
    }

    /** 最近 30 个自然日（含今天）是否打卡，索引 0=29天前 … 29=今天 */
    public List<Boolean> getRecent30DayCheckIns(String userId) {
        return getRecentDayCheckIns(userId, 30);
    }

    private List<Boolean> getRecentDayCheckIns(String userId, int days) {
        String datesKey = streakPrefix + userId + ":dates";
        List<Boolean> result = new ArrayList<>(days);
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            String d = today.minusDays(i).toString();
            Boolean member = redisTemplate.opsForSet().isMember(datesKey, d);
            result.add(Boolean.TRUE.equals(member));
        }
        return result;
    }

    public void updateLeaderboard(String userId, int points) {
        redisTemplate.opsForZSet().add(leaderboardKey, userId, points);
    }

    public List<com.jellystudy.common.entity.LeaderboardEntryDTO> getTopLeaderboard(int limit) {
        var tuples = redisTemplate.opsForZSet().reverseRangeWithScores(leaderboardKey, 0, limit - 1);
        List<com.jellystudy.common.entity.LeaderboardEntryDTO> list = new ArrayList<>();
        if (tuples == null) {
            return list;
        }
        int rank = 1;
        for (var tuple : tuples) {
            list.add(com.jellystudy.common.entity.LeaderboardEntryDTO.builder()
                    .userId(tuple.getValue())
                    .points(tuple.getScore() != null ? tuple.getScore().intValue() : 0)
                    .rank(rank++)
                    .build());
        }
        return list;
    }
}
