package com.jellystudy.qa.repository;

import com.jellystudy.qa.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, String> {

    List<Answer> findByQuestionId(String questionId);

    List<Answer> findByQuestionIdOrderByLikeCountDesc(String questionId);
}
