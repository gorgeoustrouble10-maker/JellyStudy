package com.jellystudy.coach.repository;

import com.jellystudy.coach.document.AiQuiz;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AiQuizRepository extends MongoRepository<AiQuiz, String> {

    List<AiQuiz> findByUserIdOrderByCreatedAtDesc(String userId);
}
