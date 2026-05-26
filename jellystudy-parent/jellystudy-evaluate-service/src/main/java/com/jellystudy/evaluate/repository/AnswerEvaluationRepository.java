package com.jellystudy.evaluate.repository;

import com.jellystudy.evaluate.entity.AnswerEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 答案评估记录Repository
 */
@Repository
public interface AnswerEvaluationRepository extends JpaRepository<AnswerEvaluation, String> {

    /**
     * 根据答案ID查询评估记录
     */
    Optional<AnswerEvaluation> findByAnswerId(String answerId);

    /**
     * 根据问题ID查询评估记录
     */
    List<AnswerEvaluation> findByQuestionId(String questionId);

    /**
     * 根据评分等级查询评估记录
     */
    List<AnswerEvaluation> findByGrade(String grade);

    /**
     * 根据得分范围查询评估记录
     */
    List<AnswerEvaluation> findByScoreBetween(Integer minScore, Integer maxScore);

    void deleteByQuestionId(String questionId);

    void deleteByAnswerId(String answerId);
}
