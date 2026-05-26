package com.jellystudy.common.service;

import com.jellystudy.common.entity.AiQuizDTO;
import com.jellystudy.common.entity.DailyTaskDTO;
import com.jellystudy.common.entity.GrowthProfileDTO;
import com.jellystudy.common.entity.PetStateDTO;

import java.util.List;

/**
 * JellyCoach 成长教练 Dubbo 接口
 */
public interface ICoachService {

    GrowthProfileDTO getProfile(String userId);

    List<DailyTaskDTO> getTodayTasks(String userId);

    PetStateDTO getPetState(String userId);

    PetStateDTO feedPet(String userId, int points);

    String generateWeeklyReport(String userId);

    List<AiQuizDTO> generateQuiz(String userId, String weakPoint);

    AiQuizDTO submitQuizAnswer(String quizId, String answer);

    void onEvaluationCompleted(String answerId, String questionId, int score, String grade);
}
