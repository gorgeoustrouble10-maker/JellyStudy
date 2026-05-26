package com.jellystudy.evaluate.repository;

import com.jellystudy.evaluate.entity.QuestionEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 问题评估记录Repository
 */
@Repository
public interface QuestionEvaluationRepository extends JpaRepository<QuestionEvaluation, String> {

    /**
     * 根据问题ID查询评估记录
     */
    Optional<QuestionEvaluation> findByQuestionId(String questionId);

    /**
     * 根据难度等级查询评估记录
     */
    List<QuestionEvaluation> findByDifficulty(String difficulty);

    /**
     * 根据知识点查询评估记录（模糊匹配）
     */
    List<QuestionEvaluation> findByKnowledgePointsContaining(String keyword);

    void deleteByQuestionId(String questionId);
}
