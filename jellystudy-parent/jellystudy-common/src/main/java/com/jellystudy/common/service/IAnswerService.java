package com.jellystudy.common.service;

import com.jellystudy.common.entity.AnswerDTO;
import com.jellystudy.common.entity.CommentDTO;

import java.util.List;

/**
 * 回答Dubbo服务接口
 */
public interface IAnswerService {

    List<AnswerDTO> getAll();

    List<AnswerDTO> getByQuestionId(String questionId);

    AnswerDTO getById(String id);

    AnswerDTO create(AnswerDTO answer);

    AnswerDTO update(String id, AnswerDTO answer);

    void delete(String id);

    AnswerDTO addComment(String answerId, CommentDTO comment);

    void incrementLikeCount(String answerId);

    List<AnswerDTO> getHighLikedAnswers(String questionId);

    void removeComment(String answerId, String commentId);

    long getAnswerCount();
}
