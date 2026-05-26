package com.jellystudy.coach.controller;

import com.jellystudy.coach.auth.CoachAuthInterceptor;
import com.jellystudy.coach.config.CoachGrowthProperties;
import com.jellystudy.coach.config.CoachModelProperties;
import com.jellystudy.coach.service.CoachServiceImpl;
import com.jellystudy.common.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
public class CoachController {

    private final CoachServiceImpl coachService;
    private final CoachGrowthProperties growthProperties;
    private final CoachModelProperties modelProperties;

    @GetMapping("/profile")
    public GrowthProfileDTO profile(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId) {
        return coachService.getProfile(userId);
    }

    @PostMapping("/sync-knowledge")
    public GrowthProfileDTO syncKnowledge(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId) {
        return coachService.syncFromKnowledge(userId);
    }

    @GetMapping("/tasks/today")
    public List<DailyTaskDTO> todayTasks(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId) {
        return coachService.getTodayTasks(userId);
    }

    @GetMapping("/pet")
    public PetStateDTO pet(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId) {
        return coachService.getPetState(userId);
    }

    @PostMapping("/pet/feed")
    public PetStateDTO feedPet(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId,
                               @RequestParam(defaultValue = "10") int points) {
        return coachService.feedPet(userId, points);
    }

    @PostMapping("/pet/theme")
    public PetStateDTO switchPetTheme(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId,
                                      @RequestParam String theme) {
        return coachService.switchPetTheme(userId, theme);
    }

    @GetMapping("/report/weekly")
    public WeeklyReportDTO weeklyReport(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId) {
        return coachService.buildWeeklyReportDto(userId);
    }

    @PostMapping("/quiz/generate")
    public List<AiQuizDTO> generateQuiz(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId,
                                        @RequestParam String weakPoint) {
        return coachService.generateQuiz(userId, weakPoint);
    }

    @PostMapping("/quiz/{quizId}/submit")
    public AiQuizDTO submitQuiz(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId,
                                @PathVariable String quizId,
                                @RequestBody Map<String, String> body) {
        return coachService.submitQuizAnswerForUser(userId, quizId, body.getOrDefault("answer", ""));
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntryDTO> leaderboard(@RequestParam(defaultValue = "10") int limit) {
        return coachService.getLeaderboard(limit);
    }

    @PostMapping("/socratic")
    public SocraticReplyDTO socratic(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId,
                                     @RequestParam String topic,
                                     @RequestBody SocraticChatRequest request) {
        return coachService.socraticAsk(userId, topic, request);
    }

    @GetMapping("/report/trend")
    public PointsTrendDTO pointsTrend(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId) {
        return coachService.getPointsTrend(userId);
    }

    @PostMapping("/socratic/summary")
    public SocraticSummaryDTO socraticSummary(@RequestAttribute(CoachAuthInterceptor.ATTR_USER_ID) String userId,
                                              @RequestParam String topic,
                                              @RequestBody SocraticChatRequest request) {
        return coachService.socraticSummarize(userId, topic, request.getHistory());
    }

    @GetMapping("/config")
    public Map<String, Object> runtimeConfig() {
        return Map.of(
                "dailyGoalCount", growthProperties.getDailyGoalCount(),
                "pointsPerTask", growthProperties.getPointsPerTask(),
                "pointsHighScore", growthProperties.getPointsHighScore(),
                "streakBonus", growthProperties.getStreakBonus(),
                "modelName", modelProperties.getModelName(),
                "modelTimeoutMs", modelProperties.getTimeout(),
                "source", "Nacos coach.growth.* + coach.model.*（@RefreshScope 热更新）");
    }
}
