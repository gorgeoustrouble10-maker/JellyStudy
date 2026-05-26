package com.jellystudy.qa.service;

import com.jellystudy.common.entity.AnswerDTO;
import com.jellystudy.common.entity.CommentDTO;
import com.jellystudy.common.entity.QuestionDTO;
import com.jellystudy.common.service.IAnswerService;
import com.jellystudy.qa.entity.Answer;
import com.jellystudy.qa.repository.AnswerRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 回答服务：模块内使用 QuestionServiceImpl；评估通过 Dubbo（EvaluateAsyncExecutor）
 */
@Slf4j
@DubboService(version = "1.0.0", protocol = "tri")
@Service
public class AnswerServiceImpl implements IAnswerService {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionServiceImpl questionService;

    @Autowired
    private EvaluateAsyncExecutor evaluateAsyncExecutor;

    @Autowired
    private EvaluateCleanupClient evaluateCleanupClient;

    @Override
    public List<AnswerDTO> getAll() {
        return answerRepository.findAll().stream()
                .filter(Objects::nonNull)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AnswerDTO getById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return answerRepository.findById(id).map(this::convertToDTO).orElse(null);
    }

    @Override
    public List<AnswerDTO> getByQuestionId(String questionId) {
        if (questionId == null || questionId.isEmpty()) {
            return new ArrayList<>();
        }
        return answerRepository.findByQuestionIdOrderByLikeCountDesc(questionId).stream()
                .filter(Objects::nonNull)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AnswerDTO create(AnswerDTO answerDTO) {
        if (answerDTO == null) {
            throw new IllegalArgumentException("回答DTO不能为空");
        }
        if (answerDTO.getQuestionId() == null || answerDTO.getQuestionId().isEmpty()) {
            throw new IllegalArgumentException("问题ID不能为空");
        }

        QuestionDTO question = questionService.getByIdWithoutViewIncrement(answerDTO.getQuestionId());
        if (question == null) {
            throw new IllegalArgumentException("问题不存在");
        }

        Answer answer = convertToEntity(answerDTO);
        if (answer.getId() == null || answer.getId().isEmpty()) {
            answer.setId(UUID.randomUUID().toString());
        }
        Date now = new Date();
        answer.setCreatedAt(now);
        answer.setUpdatedAt(now);
        if (answer.getComments() == null) {
            answer.setComments(new ArrayList<>());
        }
        Answer saved = answerRepository.save(answer);
        questionService.incrementAnswerCount(saved.getQuestionId());

        evaluateAsyncExecutor.evaluateAnswerAsync(
                saved.getId(),
                saved.getQuestionId(),
                question.getContent(),
                saved.getContent(),
                saved.getAuthor());

        return convertToDTO(saved);
    }

    @Override
    public AnswerDTO update(String id, AnswerDTO answerDTO) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID不能为空");
        }
        if (answerDTO == null) {
            throw new IllegalArgumentException("回答DTO不能为空");
        }
        Optional<Answer> existing = answerRepository.findById(id);
        if (existing.isEmpty()) {
            return null;
        }
        Answer updated = existing.get();
        updated.setContent(answerDTO.getContent());
        updated.setUpdatedAt(new Date());
        return convertToDTO(answerRepository.save(updated));
    }

    @Override
    public void delete(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        evaluateCleanupClient.onAnswerDeleted(id);
        answerRepository.deleteById(id);
    }

    @Override
    public AnswerDTO addComment(String answerId, CommentDTO commentDTO) {
        if (answerId == null || answerId.isEmpty()) {
            throw new IllegalArgumentException("回答ID不能为空");
        }
        if (commentDTO == null) {
            throw new IllegalArgumentException("评论不能为空");
        }
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("回答不存在"));
        if (answer.getComments() == null) {
            answer.setComments(new ArrayList<>());
        }
        answer.getComments().add(Answer.Comment.builder()
                .id(commentDTO.getId() != null ? commentDTO.getId() : UUID.randomUUID().toString())
                .content(commentDTO.getContent())
                .author(commentDTO.getAuthor())
                .createdAt(commentDTO.getCreatedAt() != null ? commentDTO.getCreatedAt() : new Date())
                .build());
        answer.setUpdatedAt(new Date());
        return convertToDTO(answerRepository.save(answer));
    }

    @Override
    public void removeComment(String answerId, String commentId) {
        if (answerId == null || commentId == null) {
            return;
        }
        answerRepository.findById(answerId).ifPresent(answer -> {
            if (answer.getComments() != null) {
                answer.getComments().removeIf(c -> commentId.equals(c.getId()));
                answer.setUpdatedAt(new Date());
                answerRepository.save(answer);
            }
        });
    }

    @Override
    public void incrementLikeCount(String answerId) {
        if (answerId == null || answerId.isEmpty()) {
            return;
        }
        answerRepository.findById(answerId).ifPresent(a -> {
            a.setLikeCount(a.getLikeCount() + 1);
            a.setUpdatedAt(new Date());
            answerRepository.save(a);
        });
    }

    @Override
    public List<AnswerDTO> getHighLikedAnswers(String questionId) {
        return getByQuestionId(questionId);
    }

    @Override
    public long getAnswerCount() {
        return answerRepository.count();
    }

    private AnswerDTO convertToDTO(Answer entity) {
        if (entity == null) {
            return null;
        }
        List<CommentDTO> commentDtos = new ArrayList<>();
        if (entity.getComments() != null) {
            for (Answer.Comment c : entity.getComments()) {
                commentDtos.add(CommentDTO.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .author(c.getAuthor())
                        .createdAt(c.getCreatedAt())
                        .build());
            }
        }
        return AnswerDTO.builder()
                .id(entity.getId())
                .questionId(entity.getQuestionId())
                .content(entity.getContent())
                .author(entity.getAuthor())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .likeCount(entity.getLikeCount())
                .comments(commentDtos)
                .build();
    }

    private Answer convertToEntity(AnswerDTO dto) {
        if (dto == null) {
            return null;
        }
        List<Answer.Comment> comments = new ArrayList<>();
        if (dto.getComments() != null) {
            for (CommentDTO c : dto.getComments()) {
                comments.add(Answer.Comment.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .author(c.getAuthor())
                        .createdAt(c.getCreatedAt())
                        .build());
            }
        }
        return Answer.builder()
                .id(dto.getId())
                .questionId(dto.getQuestionId())
                .content(dto.getContent())
                .author(dto.getAuthor())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .likeCount(dto.getLikeCount())
                .comments(comments)
                .build();
    }
}
