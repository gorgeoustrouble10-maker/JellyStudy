package com.jellystudy.qa.repository;

import com.jellystudy.qa.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, String> {

    List<Question> findByKnowledgePointIdOrderByCreatedAtDesc(String knowledgePointId);

    List<Question> findAllByOrderByCreatedAtDesc();

    List<Question> findTop10ByOrderByLikeCountDesc();

    List<Question> findTop10ByOrderByViewCountDesc();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Question q SET q.viewCount = q.viewCount + 1 WHERE q.id = :id")
    int incrementViewCount(@Param("id") String id);

    @Query("""
            SELECT q FROM Question q
            WHERE LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(q.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY q.updatedAt DESC
            """)
    List<Question> searchByKeyword(@Param("keyword") String keyword);
}
