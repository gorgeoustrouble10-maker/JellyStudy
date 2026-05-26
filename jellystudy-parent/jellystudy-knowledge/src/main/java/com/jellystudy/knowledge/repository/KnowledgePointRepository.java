package com.jellystudy.knowledge.repository;

import com.jellystudy.knowledge.entity.KnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint, String> {
}
