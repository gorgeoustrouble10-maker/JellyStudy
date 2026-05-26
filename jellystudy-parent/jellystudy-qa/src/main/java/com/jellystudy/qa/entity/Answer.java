package com.jellystudy.qa.entity;

import com.jellystudy.qa.converter.CommentListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "answer")
public class Answer {

    @Id
    private String id;

    @Column(name = "question_id", nullable = false, length = 36)
    private String questionId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String author;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @Column(name = "like_count")
    private int likeCount;

    @Column(name = "comments_json", columnDefinition = "TEXT")
    @Convert(converter = CommentListJsonConverter.class)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Comment {
        private String id;
        private String content;
        private String author;
        private Date createdAt;
    }
}
