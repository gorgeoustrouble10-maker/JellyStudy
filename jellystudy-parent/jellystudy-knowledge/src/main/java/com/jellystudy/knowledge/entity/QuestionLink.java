package com.jellystudy.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * 只读映射 question 表，用于统计知识点下的问题数量
 */
@Getter
@Entity
@Immutable
@Table(name = "question")
public class QuestionLink {

    @Id
    private String id;

    @Column(name = "knowledge_point_id", length = 36)
    private String knowledgePointId;
}
