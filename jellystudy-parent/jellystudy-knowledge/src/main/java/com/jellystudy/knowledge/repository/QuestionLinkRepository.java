package com.jellystudy.knowledge.repository;

import com.jellystudy.knowledge.entity.QuestionLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionLinkRepository extends JpaRepository<QuestionLink, String> {

    long countByKnowledgePointId(String knowledgePointId);
}
