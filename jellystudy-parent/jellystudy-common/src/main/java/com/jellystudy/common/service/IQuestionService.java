package com.jellystudy.common.service;

import com.jellystudy.common.entity.QuestionDTO;

import java.util.List;

/**
 * 问题Dubbo服务接口
 */
public interface IQuestionService {

    List<QuestionDTO> getAll();

    QuestionDTO getById(String id);

    QuestionDTO create(QuestionDTO question);

    QuestionDTO update(String id, QuestionDTO question);

    void delete(String id);

    List<QuestionDTO> getByKnowledgePointId(String knowledgePointId);

    List<QuestionDTO> getHotQuestions();

    List<QuestionDTO> getRecommendedQuestions();

    long getQuestionCount();

    void incrementAnswerCount(String questionId);

    void incrementLikeCount(String questionId);

    /**
     * 按标题/内容关键字搜索（忽略大小写）。
     */
    List<QuestionDTO> search(String keyword);
}
