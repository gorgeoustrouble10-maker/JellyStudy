package com.jellystudy.qa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "question")
public class Question {

    @Id
    private String id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "knowledge_point_id", length = 36)
    private String knowledgePointId;

    @Column(length = 100)
    private String author;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @Column(name = "view_count")
    private int viewCount;

    @Column(name = "like_count")
    private int likeCount;

    @Column(name = "answer_count")
    private int answerCount;
}
