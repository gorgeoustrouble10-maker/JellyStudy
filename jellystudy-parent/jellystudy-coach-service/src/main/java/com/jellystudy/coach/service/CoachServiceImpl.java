package com.jellystudy.coach.service;

import com.jellystudy.coach.ai.CoachAiEngine;
import com.jellystudy.coach.config.CoachGrowthProperties;
import com.jellystudy.coach.document.AiQuiz;
import com.jellystudy.coach.document.GrowthProfile;
import com.jellystudy.coach.document.PetState;
import com.jellystudy.coach.document.SocraticSession;
import com.jellystudy.coach.document.WeeklySnapshot;
import com.jellystudy.coach.redis.CoachRedisCache;
import com.jellystudy.coach.repository.AiQuizRepository;
import com.jellystudy.coach.repository.GrowthProfileRepository;
import com.jellystudy.coach.repository.PetStateRepository;
import com.jellystudy.coach.repository.SocraticSessionRepository;
import com.jellystudy.coach.repository.WeeklySnapshotRepository;
import com.jellystudy.common.entity.*;
import com.jellystudy.common.service.ICoachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.stream.Collectors;

@Slf4j
@Service
@DubboService(version = "1.0.0", protocol = "tri")
@RequiredArgsConstructor
public class CoachServiceImpl implements ICoachService {

    private final GrowthProfileRepository growthProfileRepository;
    private final PetStateRepository petStateRepository;
    private final AiQuizRepository aiQuizRepository;
    private final CoachAiEngine coachAiEngine;
    private final CoachRedisCache coachRedisCache;
    private final CoachGrowthProperties growthProperties;
    private final KnowledgeScopeService knowledgeScopeService;
    private final SocraticSessionRepository socraticSessionRepository;
    private final WeeklySnapshotRepository weeklySnapshotRepository;
    private final EvaluateBridgeService evaluateBridgeService;
    private final KnowledgeMasteryBuilder knowledgeMasteryBuilder;

    @Override
    public GrowthProfileDTO getProfile(String userId) {
        GrowthProfile profile = ensureProfile(userId);
        grantWelcomeBonusIfNeeded(profile);
        syncKnowledgeScope(profile);
        return toProfileDto(profile);
    }

    @Override
    public List<DailyTaskDTO> getTodayTasks(String userId) {
        List<DailyTaskDTO> cached = coachRedisCache.getTodayTasks(userId);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        GrowthProfile profile = ensureProfile(userId);
        syncKnowledgeScope(profile);
        List<String> weakPoints = resolveWeakPoints(profile);

        int goalCount = growthProperties.getDailyGoalCount();
        List<DailyTaskDTO> tasks = new ArrayList<>();
        for (int i = 0; i < goalCount; i++) {
            String weak = weakPoints.get(i % weakPoints.size());
            tasks.add(DailyTaskDTO.builder()
                    .taskId(UUID.randomUUID().toString())
                    .title("巩固练习：" + weak + "（来自知识点库）")
                    .weakPoint(weak)
                    .rewardPoints(growthProperties.getPointsPerTask())
                    .completed(false)
                    .build());
        }
        coachRedisCache.cacheTodayTasks(userId, tasks);
        return tasks;
    }

    @Override
    public PetStateDTO getPetState(String userId) {
        PetState pet = ensurePet(userId);
        syncPetThemes(pet);
        return toPetDto(pet);
    }

    @Override
    public PetStateDTO feedPet(String userId, int points) {
        PetState pet = ensurePet(userId);
        GrowthProfile profile = ensureProfile(userId);
        grantWelcomeBonusIfNeeded(profile);
        if (profile.getTotalPoints() < points) {
            throw new IllegalArgumentException(
                    "积分不足（当前 " + profile.getTotalPoints() + " 分）。请先完成下方 AI 练习获取积分。");
        }
        profile.setTotalPoints(profile.getTotalPoints() - points);
        pet.setExperience(pet.getExperience() + points);
        while (pet.getExperience() >= expForLevel(pet.getLevel())) {
            pet.setExperience(pet.getExperience() - expForLevel(pet.getLevel()));
            pet.setLevel(pet.getLevel() + 1);
            pet.setMood("excited");
        }
        growthProfileRepository.save(profile);
        petStateRepository.save(pet);
        coachRedisCache.updateLeaderboard(userId, profile.getTotalPoints());
        return toPetDto(pet);
    }

    public PetStateDTO switchPetTheme(String userId, String theme) {
        PetState pet = ensurePet(userId);
        syncPetThemes(pet);
        if (theme == null || theme.isBlank() || "default".equalsIgnoreCase(theme) || "原始形态".equals(theme)) {
            pet.setCurrentTheme(null);
            pet.setMood("happy");
        } else {
            List<String> unlocked = pet.getUnlockedThemes() != null ? pet.getUnlockedThemes() : List.of();
            boolean allowed = unlocked.stream()
                    .anyMatch(t -> t.equals(theme) || t.contains(theme) || theme.contains(t));
            if (!allowed) {
                throw new IllegalArgumentException("主题「" + theme + "」尚未解锁，请先完成对应知识点练习（≥80分）。");
            }
            pet.setCurrentTheme(unlocked.stream()
                    .filter(t -> t.equals(theme) || t.contains(theme) || theme.contains(t))
                    .findFirst().orElse(theme));
            pet.setMood("proud");
        }
        petStateRepository.save(pet);
        return toPetDto(pet);
    }

    @Override
    public String generateWeeklyReport(String userId) {
        return buildWeeklyReportDto(userId).getAiSummary();
    }

    public WeeklyReportDTO buildWeeklyReportDto(String userId) {
        GrowthProfile profile = ensureProfile(userId);
        syncKnowledgeScope(profile);
        int streak = coachRedisCache.getStreak(userId);
        List<String> learned = profile.getLearnedKnowledgePoints() != null
                ? profile.getLearnedKnowledgePoints() : List.of();
        List<String> weak = resolveWeakPoints(profile);
        int weeklyGoal = growthProperties.getDailyGoalCount() * 7 * growthProperties.getPointsPerTask();
        String summary = coachAiEngine.generateWeeklyReport(
                userId, profile.getTotalPoints(), streak, weak, learned);
        summary = sanitizeReportMarkdown(summary);
        summary = appendSocraticToSummary(summary, userId);
        saveWeeklySnapshot(userId, profile.getTotalPoints(), streak);
        return WeeklyReportDTO.builder()
                .userId(userId)
                .totalPoints(profile.getTotalPoints())
                .streakDays(streak)
                .checkedInToday(coachRedisCache.hasCheckedInToday(userId))
                .recent7DayCheckIns(coachRedisCache.getRecent7DayCheckIns(userId))
                .recent30DayCheckIns(coachRedisCache.getRecent30DayCheckIns(userId))
                .weeklyGoalPoints(weeklyGoal)
                .learnedKnowledgePoints(learned)
                .weakPoints(weak)
                .recentSocraticSessions(loadRecentSocraticBriefs(userId))
                .knowledgeMastery(buildKnowledgeMastery(userId, learned, weak))
                .pointsTrend(buildPointsTrend(userId))
                .aiSummary(summary)
                .generatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
                .build();
    }

    public PointsTrendDTO getPointsTrend(String userId) {
        return buildPointsTrend(userId);
    }

    private PointsTrendDTO buildPointsTrend(String userId) {
        List<WeeklySnapshot> snaps = weeklySnapshotRepository.findTop12ByUserIdOrderByCreatedAtDesc(userId);
        Collections.reverse(snaps);
        List<String> labels = new ArrayList<>();
        List<Integer> points = new ArrayList<>();
        for (WeeklySnapshot s : snaps) {
            labels.add(s.getWeekKey());
            points.add(s.getTotalPoints());
        }
        return PointsTrendDTO.builder().labels(labels).points(points).build();
    }

    private void saveWeeklySnapshot(String userId, int totalPoints, int streak) {
        LocalDate now = LocalDate.now();
        WeekFields wf = WeekFields.of(Locale.CHINA);
        String weekKey = now.getYear() + "-W" + String.format("%02d", now.get(wf.weekOfWeekBasedYear()));
        weeklySnapshotRepository.findByUserIdAndWeekKey(userId, weekKey).ifPresentOrElse(existing -> {
            existing.setTotalPoints(totalPoints);
            existing.setStreakDays(streak);
            existing.setCreatedAt(new Date());
            weeklySnapshotRepository.save(existing);
        }, () -> weeklySnapshotRepository.save(WeeklySnapshot.builder()
                .userId(userId)
                .weekKey(weekKey)
                .totalPoints(totalPoints)
                .streakDays(streak)
                .createdAt(new Date())
                .build()));
    }

    private List<SocraticSessionBriefDTO> loadRecentSocraticBriefs(String userId) {
        return socraticSessionRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(s -> SocraticSessionBriefDTO.builder()
                        .id(s.getId())
                        .topic(s.getTopic())
                        .keyPoints(s.getKeyPoints())
                        .misconceptions(s.getMisconceptions())
                        .evaluateScore(s.getEvaluateScore())
                        .evaluateGrade(s.getEvaluateGrade())
                        .createdAt(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(s.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    private String appendSocraticToSummary(String summary, String userId) {
        List<SocraticSessionBriefDTO> recent = loadRecentSocraticBriefs(userId);
        if (recent.isEmpty()) {
            return summary;
        }
        StringBuilder sb = new StringBuilder(summary);
        sb.append("\n\n## 近期苏格拉底学习（MongoDB）\n");
        for (SocraticSessionBriefDTO s : recent) {
            sb.append("- **").append(s.getTopic()).append("**");
            if (s.getEvaluateScore() != null) {
                sb.append(" · 评估 ").append(s.getEvaluateScore()).append(" 分");
                if (s.getEvaluateGrade() != null) {
                    sb.append("（").append(s.getEvaluateGrade()).append("）");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String sanitizeReportMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?s)^```(?:markdown)?\\s*", "")
                .replaceAll("```\\s*$", "")
                .trim();
    }

    @Override
    public List<AiQuizDTO> generateQuiz(String userId, String weakPoint) {
        GrowthProfile profile = ensureProfile(userId);
        syncKnowledgeScope(profile);
        List<String> scope = profile.getLearnedKnowledgePoints() != null
                ? profile.getLearnedKnowledgePoints() : knowledgeScopeService.loadNames();
        if (!knowledgeScopeService.isInScope(weakPoint, scope)) {
            throw new IllegalArgumentException("「" + weakPoint + "」不在您的知识点库中，请从已学知识点选择练习。");
        }
        String scopeDetail = knowledgeScopeService.buildScopeDescription(knowledgeScopeService.loadAll());
        List<AiQuiz> quizzes = coachAiEngine.generateQuizzes(userId, weakPoint, 1, scope, scopeDetail);
        aiQuizRepository.saveAll(quizzes);
        return quizzes.stream().map(this::toQuizDto).collect(Collectors.toList());
    }

    @Override
    public AiQuizDTO submitQuizAnswer(String quizId, String answer) {
        return submitQuizAnswerForUser(null, quizId, answer);
    }

    public AiQuizDTO submitQuizAnswerForUser(String userId, String quizId, String answer) {
        AiQuiz quiz = aiQuizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("题目不存在"));
        if (userId != null && quiz.getUserId() != null && !quiz.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权提交他人的练习");
        }
        String owner = quiz.getUserId() != null ? quiz.getUserId() : userId;
        CoachAiEngine.QuizGradeResult grade = coachAiEngine.gradeQuiz(
                quiz.getQuestion(), answer, quiz.getWeakPoint());
        quiz.setUserAnswer(answer);
        quiz.setScore(grade.score());
        quiz.setFeedback(grade.feedback());
        quiz.setStatus("DONE");
        aiQuizRepository.save(quiz);

        GrowthProfile profile = ensureProfile(owner);
        int earned = growthProperties.getPointsPerTask();
        if (grade.score() >= 80) {
            earned += growthProperties.getPointsHighScore();
            unlockPetTheme(owner, quiz.getWeakPoint());
        }
        profile.setTotalPoints(profile.getTotalPoints() + earned);
        profile.setUpdatedAt(new Date());
        growthProfileRepository.save(profile);
        coachRedisCache.recordDailyCheckIn(owner);
        coachRedisCache.updateLeaderboard(owner, profile.getTotalPoints());

        return toQuizDto(quiz);
    }

    @Override
    public void onEvaluationCompleted(String answerId, String questionId, int score, String grade) {
        onEvaluationCompleted(answerId, questionId, score, grade, null);
    }

    public void onEvaluationCompleted(String answerId, String questionId, int score, String grade, String userId) {
        String resolved = (userId != null && !userId.isBlank()) ? userId.trim() : "demo-user";
        GrowthProfile profile = ensureProfile(resolved);
        syncKnowledgeScope(profile);
        List<String> scope = profile.getLearnedKnowledgePoints() != null
                ? profile.getLearnedKnowledgePoints() : List.of();

        List<String> summaries = new ArrayList<>();
        if (profile.getLastDiagnosis() != null) {
            summaries.add(profile.getLastDiagnosis());
        }
        summaries.add("答案评估 answerId=" + answerId + " questionId=" + questionId + " score=" + score + " grade=" + grade);

        CoachAiEngine.DiagnosisResult diagnosis = coachAiEngine.diagnoseWeakPoints(summaries, scope);
        profile.setWeakPoints(diagnosis.weakPoints());
        profile.setLastDiagnosis(diagnosis.diagnosis());
        if (score >= 80) {
            profile.setTotalPoints(profile.getTotalPoints() + growthProperties.getPointsHighScore());
        }
        profile.setUpdatedAt(new Date());
        growthProfileRepository.save(profile);
        coachRedisCache.updateLeaderboard(resolved, profile.getTotalPoints());
        coachRedisCache.invalidateTodayTasks(resolved);
        log.info("Coach 已处理评估事件 userId={} questionId={} score={}", resolved, questionId, score);
    }

    private List<KnowledgeMasteryDTO> buildKnowledgeMastery(String userId,
                                                            List<String> learned,
                                                            List<String> weak) {
        List<AiQuiz> quizzes = aiQuizRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<SocraticSession> sessions = socraticSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return knowledgeMasteryBuilder.build(learned, weak, quizzes, sessions);
    }

    public GrowthProfileDTO syncFromKnowledge(String userId) {
        GrowthProfile profile = ensureProfile(userId);
        syncKnowledgeScope(profile);
        coachRedisCache.invalidateTodayTasks(userId);
        return toProfileDto(profile);
    }

    public List<LeaderboardEntryDTO> getLeaderboard(int limit) {
        return coachRedisCache.getTopLeaderboard(Math.min(limit, 20));
    }

    public SocraticReplyDTO socraticAsk(String userId, String topic, SocraticChatRequest request) {
        GrowthProfile profile = ensureProfile(userId);
        syncKnowledgeScope(profile);
        List<String> scope = profile.getLearnedKnowledgePoints() != null
                ? profile.getLearnedKnowledgePoints() : List.of();
        if (!knowledgeScopeService.isInScope(topic, scope)) {
            throw new IllegalArgumentException("请选择已学知识点进行对话：" + String.join("、", scope));
        }
        String message = request.getMessage() != null ? request.getMessage().trim() : "";
        if (message.isEmpty()) {
            throw new IllegalArgumentException("请输入要追问的内容");
        }
        List<SocraticMessageDTO> history = request.getHistory() != null ? request.getHistory() : List.of();
        String detail = knowledgeScopeService.buildScopeDescription(knowledgeScopeService.loadAll());
        return coachAiEngine.socraticGuide(topic, message, history, scope, detail);
    }

    public SocraticSummaryDTO socraticSummarize(String userId, String topic, List<SocraticMessageDTO> history) {
        GrowthProfile profile = ensureProfile(userId);
        syncKnowledgeScope(profile);
        List<String> scope = profile.getLearnedKnowledgePoints() != null
                ? profile.getLearnedKnowledgePoints() : List.of();
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("请先进行至少一轮追问再生成总结");
        }
        SocraticSummaryDTO summary = coachAiEngine.socraticSummarize(topic, history, scope);

        String transcript = formatHistoryTranscript(history);
        AnswerEvaluationDTO eval = evaluateBridgeService.evaluateSocraticDialogue(topic, transcript);
        if (eval != null) {
            List<String> misconceptions = new ArrayList<>(
                    summary.getMisconceptions() != null ? summary.getMisconceptions() : List.of());
            if (eval.getSuggestions() != null) {
                misconceptions.addAll(eval.getSuggestions().stream().limit(2).toList());
            }
            summary.setMisconceptions(misconceptions);
            if (eval.getEvaluationDetails() != null) {
                summary.setLogicChainComment(
                        (summary.getLogicChainComment() != null ? summary.getLogicChainComment() + " " : "")
                                + "【评估服务 Dubbo】" + eval.getEvaluationDetails());
            }
        }

        SocraticSession session = SocraticSession.builder()
                .userId(userId)
                .topic(topic)
                .keyPoints(summary.getKeyPoints())
                .masteredAspects(summary.getMasteredAspects())
                .misconceptions(summary.getMisconceptions())
                .logicChainComment(summary.getLogicChainComment())
                .recommendedPractice(summary.getRecommendedPractice())
                .summaryMarkdown(summary.getSummaryMarkdown())
                .evaluateScore(eval != null ? eval.getScore() : null)
                .evaluateGrade(eval != null ? eval.getGrade() : null)
                .evaluateDetails(eval != null ? eval.getEvaluationDetails() : null)
                .createdAt(new Date())
                .build();
        socraticSessionRepository.save(session);

        List<String> evalLines = new ArrayList<>();
        evalLines.add("苏格拉底对话 topic=" + topic + " turns=" + history.size());
        if (!summary.getMisconceptions().isEmpty()) {
            evalLines.add("误区：" + String.join("；", summary.getMisconceptions()));
        }
        if (!summary.getMasteredAspects().isEmpty()) {
            evalLines.add("掌握：" + String.join("；", summary.getMasteredAspects()));
        }
        if (eval != null) {
            evalLines.add("评估服务 score=" + eval.getScore() + " grade=" + eval.getGrade());
        }
        CoachAiEngine.DiagnosisResult diagnosis = coachAiEngine.diagnoseWeakPoints(evalLines, scope);
        profile.setWeakPoints(diagnosis.weakPoints());
        profile.setLastDiagnosis("【苏格拉底评估】" + diagnosis.diagnosis());
        profile.setUpdatedAt(new Date());
        growthProfileRepository.save(profile);
        coachRedisCache.invalidateTodayTasks(userId);
        if (eval != null) {
            summary.setEvaluateScore(eval.getScore());
            summary.setEvaluateGrade(eval.getGrade());
        }
        return summary;
    }

    private String formatHistoryTranscript(List<SocraticMessageDTO> history) {
        StringBuilder sb = new StringBuilder();
        for (SocraticMessageDTO m : history) {
            if (m.getContent() == null || m.getContent().isBlank()) {
                continue;
            }
            sb.append("user".equals(m.getRole()) ? "学生：" : "助教：")
                    .append(m.getContent().trim()).append("\n");
        }
        return sb.toString();
    }

    private void syncKnowledgeScope(GrowthProfile profile) {
        List<KnowledgePointDTO> kps = knowledgeScopeService.loadAll();
        List<String> names = kps.stream().map(KnowledgePointDTO::getName).collect(Collectors.toList());
        profile.setLearnedKnowledgePoints(names);
        if (names.isEmpty()) {
            profile.setLastDiagnosis("请先在「知识点管理」中添加知识点，Coach 才能基于您的学习内容出题。");
            growthProfileRepository.save(profile);
            return;
        }
        List<String> currentWeak = profile.getWeakPoints();
        if (currentWeak == null || currentWeak.isEmpty()
                || currentWeak.stream().anyMatch(w -> w.contains("Spring Boot") || w.contains("微服务"))) {
            profile.setWeakPoints(topFromList(names, 3));
            profile.setLastDiagnosis("已同步您的 " + names.size() + " 个知识点："
                    + String.join("、", names) + "。练习将严格限定在此范围内。");
        } else {
            profile.setWeakPoints(filterToKnown(currentWeak, names));
        }
        growthProfileRepository.save(profile);
    }

    private List<String> resolveWeakPoints(GrowthProfile profile) {
        List<String> learned = profile.getLearnedKnowledgePoints();
        if (learned != null && !learned.isEmpty()) {
            List<String> weak = profile.getWeakPoints();
            if (weak != null && !weak.isEmpty()) {
                List<String> filtered = filterToKnown(weak, learned);
                if (!filtered.isEmpty()) {
                    return filtered;
                }
            }
            return topFromList(learned, growthProperties.getDailyGoalCount());
        }
        return List.of("请先在知识点管理中添加内容");
    }

    private List<String> filterToKnown(List<String> weak, List<String> learned) {
        return weak.stream()
                .filter(w -> learned.stream().anyMatch(k -> k.equals(w) || k.contains(w) || w.contains(k)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<String> topFromList(List<String> list, int n) {
        return list.subList(0, Math.min(n, list.size()));
    }

    private void unlockPetTheme(String userId, String theme) {
        PetState pet = ensurePet(userId);
        List<String> themes = pet.getUnlockedThemes() != null
                ? new ArrayList<>(pet.getUnlockedThemes()) : new ArrayList<>();
        if (!themes.contains(theme)) {
            themes.add(theme);
            pet.setUnlockedThemes(themes);
            pet.setMood("proud");
            petStateRepository.save(pet);
            log.info("解锁宠物主题: {} for {}（可在宠物卡片切换形态）", theme, userId);
        }
    }

    private void syncPetThemes(PetState pet) {
        if (pet.getUnlockedThemes() == null) {
            pet.setUnlockedThemes(new ArrayList<>());
        }
    }

    private void grantWelcomeBonusIfNeeded(GrowthProfile profile) {
        if (profile.getTotalPoints() < 10) {
            profile.setTotalPoints(30);
            profile.setUpdatedAt(new Date());
            growthProfileRepository.save(profile);
            coachRedisCache.updateLeaderboard(profile.getUserId(), profile.getTotalPoints());
        }
    }

    private GrowthProfile ensureProfile(String userId) {
        List<GrowthProfile> existing = growthProfileRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);
        if (existing.size() > 1) {
            for (int i = 1; i < existing.size(); i++) {
                growthProfileRepository.delete(existing.get(i));
            }
            log.warn("已清理 userId={} 的重复 growth_profile {} 条", userId, existing.size() - 1);
        }
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        GrowthProfile p = GrowthProfile.builder()
                .userId(userId)
                .totalPoints(30)
                .streakDays(0)
                .weakPoints(new ArrayList<>())
                .learnedKnowledgePoints(new ArrayList<>())
                .lastDiagnosis("欢迎加入 JellyCoach！正在同步您的知识点…")
                .updatedAt(new Date())
                .build();
        return growthProfileRepository.save(p);
    }

    private PetState ensurePet(String userId) {
        List<PetState> existing = petStateRepository.findAllByUserId(userId);
        if (existing.size() > 1) {
            for (int i = 1; i < existing.size(); i++) {
                petStateRepository.delete(existing.get(i));
            }
            log.warn("已清理 userId={} 的重复 pet_state {} 条", userId, existing.size() - 1);
        }
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        PetState pet = PetState.builder()
                .userId(userId)
                .petName("小果冻")
                .species("jelly-dragon")
                .level(1)
                .experience(0)
                .mood("happy")
                .unlockedThemes(new ArrayList<>())
                .currentTheme(null)
                .build();
        return petStateRepository.save(pet);
    }

    private int expForLevel(int level) {
        return 50 + level * 20;
    }

    private GrowthProfileDTO toProfileDto(GrowthProfile p) {
        List<String> learned = p.getLearnedKnowledgePoints() != null ? p.getLearnedKnowledgePoints() : List.of();
        int streak = coachRedisCache.getStreak(p.getUserId());
        boolean today = coachRedisCache.hasCheckedInToday(p.getUserId());
        String streakNote = today
                ? "今日已打卡 · 连续 " + streak + " 天"
                : (streak > 0 ? "今日未打卡 · 当前连续 " + streak + " 天" : "今日未打卡 · 完成一次练习即可打卡");
        return GrowthProfileDTO.builder()
                .userId(p.getUserId())
                .totalPoints(p.getTotalPoints())
                .streakDays(streak)
                .checkedInToday(today)
                .recent7DayCheckIns(coachRedisCache.getRecent7DayCheckIns(p.getUserId()))
                .recent30DayCheckIns(coachRedisCache.getRecent30DayCheckIns(p.getUserId()))
                .streakNote(streakNote)
                .weakPoints(resolveWeakPoints(p))
                .learnedKnowledgePoints(learned)
                .knowledgeMastery(buildKnowledgeMastery(p.getUserId(), learned, resolveWeakPoints(p)))
                .quizScopeSource("知识点服务 Dubbo → " + learned.size() + " 项")
                .lastDiagnosis(p.getLastDiagnosis())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private PetStateDTO toPetDto(PetState pet) {
        int next = expForLevel(pet.getLevel());
        List<String> themes = pet.getUnlockedThemes() != null ? pet.getUnlockedThemes() : List.of();
        String base = resolveBasePetAppearance(pet);
        String appearance = pet.getCurrentTheme() != null ? "theme" : base;
        return PetStateDTO.builder()
                .userId(pet.getUserId())
                .petName(pet.getPetName())
                .species(pet.getSpecies())
                .level(pet.getLevel())
                .experience(pet.getExperience())
                .experienceToNext(next)
                .mood(pet.getMood())
                .appearance(appearance)
                .baseAppearance(base)
                .baseAppearanceLabel(baseAppearanceLabel(base, pet.getLevel()))
                .unlockedThemes(themes)
                .currentTheme(pet.getCurrentTheme())
                .build();
    }

    private String resolveBasePetAppearance(PetState pet) {
        return switch (Math.min(pet.getLevel(), 5)) {
            case 1 -> "egg";
            case 2 -> "chick";
            case 3 -> "juvenile";
            case 4 -> "teen";
            default -> "dragon";
        };
    }

    private String baseAppearanceLabel(String base, int level) {
        return switch (base) {
            case "egg" -> "Lv." + level + " 初生";
            case "chick" -> "Lv." + level + " 幼鸟";
            case "juvenile" -> "Lv." + level + " 少年";
            case "teen" -> "Lv." + level + " 青年";
            case "dragon" -> "Lv." + level + " 完全体";
            default -> "Lv." + level;
        };
    }

    private AiQuizDTO toQuizDto(AiQuiz q) {
        return AiQuizDTO.builder()
                .id(q.getId())
                .userId(q.getUserId())
                .weakPoint(q.getWeakPoint())
                .question(q.getQuestion())
                .hint(q.getHint())
                .userAnswer(q.getUserAnswer())
                .score(q.getScore())
                .feedback(q.getFeedback())
                .status(q.getStatus())
                .createdAt(q.getCreatedAt())
                .build();
    }
}
