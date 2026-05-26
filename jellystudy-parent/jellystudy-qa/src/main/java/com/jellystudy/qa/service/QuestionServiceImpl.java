package com.jellystudy.qa.service;

import com.jellystudy.common.entity.KnowledgePointDTO;
import com.jellystudy.common.entity.QuestionDTO;
import com.jellystudy.common.service.IKnowledgePointService;
import com.jellystudy.common.service.IQuestionService;
import com.jellystudy.qa.entity.Question;
import com.jellystudy.qa.entity.Answer;
import com.jellystudy.qa.repository.AnswerRepository;
import com.jellystudy.qa.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 问题Dubbo服务实现
 */
@Slf4j
@DubboService(version = "1.0.0", protocol = "tri")
@Service("questionServiceImpl")
public class QuestionServiceImpl implements IQuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private com.jellystudy.qa.redis.QuestionRedisService questionRedisService;

    @DubboReference(version = "1.0.0", protocol = "tri", check = false, timeout = 5000)
    private IKnowledgePointService knowledgePointService;

    @Autowired
    private EvaluateAsyncExecutor evaluateAsyncExecutor;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private EvaluateCleanupClient evaluateCleanupClient;

    @Override
    public List<QuestionDTO> getAll() {
        return questionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(Objects::nonNull)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 只读查询（不增加浏览量），供本模块内创建回答等场景使用。
     */
    public QuestionDTO getByIdWithoutViewIncrement(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return questionRepository.findById(id).map(this::convertToDTO).orElse(null);
    }

    /**
     * 查看问题详情：浏览量以 MySQL 为准（+1 后写回 Redis）。
     * 若库中已无记录但 Redis 仍有详情缓存，则返回缓存（降级）。
     * 列表「热门/常看」走 Redis ZSET，不经过本方法。
     */
    @Override
    @Transactional
    public QuestionDTO getById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        Optional<Question> question = questionRepository.findById(id);
        if (question.isEmpty()) {
            return questionRedisService.getCachedQuestion(id);
        }
        Question entity = question.get();
        questionRepository.incrementViewCount(id);
        entity.setViewCount(entity.getViewCount() + 1);
        entity.setUpdatedAt(new Date());
        questionRedisService.onQuestionViewed(entity);
        return convertToDTO(entity);
    }

    @Override
    public QuestionDTO create(QuestionDTO questionDTO) {
        if (questionDTO == null) {
            throw new IllegalArgumentException("问题DTO不能为空");
        }
        
        // 知识点校验（Dubbo 失败时不阻塞提问，仅记录日志）
        if (questionDTO.getKnowledgePointId() != null && !questionDTO.getKnowledgePointId().isEmpty()
                && knowledgePointService != null) {
            try {
                KnowledgePointDTO knowledgePoint = knowledgePointService.getById(questionDTO.getKnowledgePointId());
                if (knowledgePoint == null) {
                    throw new IllegalArgumentException("知识点不存在");
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.warn("知识点 Dubbo 校验跳过, knowledgePointId={}, reason={}",
                        questionDTO.getKnowledgePointId(), e.getMessage());
            }
        }
        
        Question question = convertToEntity(questionDTO);
        if (question.getId() == null || question.getId().isEmpty()) {
            question.setId(java.util.UUID.randomUUID().toString());
        }
        question.setCreatedAt(new Date());
        question.setUpdatedAt(new Date());
        question.setViewCount(0);
        question.setLikeCount(0);
        question.setAnswerCount(0);
        Question saved = questionRepository.save(question);
        questionRedisService.onQuestionCreated(saved);

        evaluateAsyncExecutor.evaluateQuestionAsync(saved.getId(), saved.getTitle(), saved.getContent());

        return convertToDTO(saved);
    }

    @Override
    public QuestionDTO update(String id, QuestionDTO questionDTO) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID不能为空");
        }
        if (questionDTO == null) {
            throw new IllegalArgumentException("问题DTO不能为空");
        }
        Optional<Question> existing = questionRepository.findById(id);
        if (existing.isPresent()) {
            Question updated = existing.get();
            updated.setTitle(questionDTO.getTitle());
            updated.setContent(questionDTO.getContent());
            
            // 验证新的知识点ID是否有效
            if (questionDTO.getKnowledgePointId() != null && !questionDTO.getKnowledgePointId().isEmpty()) {
                if (knowledgePointService != null) {
                    KnowledgePointDTO knowledgePoint = knowledgePointService.getById(questionDTO.getKnowledgePointId());
                    if (knowledgePoint == null) {
                        throw new IllegalArgumentException("知识点不存在");
                    }
                    updated.setKnowledgePointId(questionDTO.getKnowledgePointId());
                }
            }
            
            updated.setUpdatedAt(new Date());
            Question saved = questionRepository.save(updated);
            questionRedisService.onQuestionUpdated(saved);
            return convertToDTO(saved);
        }
        return null;
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        evaluateCleanupClient.onQuestionDeleted(id);
        List<Answer> answers = answerRepository.findByQuestionId(id);
        for (Answer answer : answers) {
            answerRepository.deleteById(answer.getId());
        }
        questionRepository.deleteById(id);
        questionRedisService.evictQuestion(id);
    }

    @Override
    public List<QuestionDTO> getByKnowledgePointId(String knowledgePointId) {
        if (knowledgePointId == null || knowledgePointId.isEmpty()) {
            return new ArrayList<>();
        }
        return questionRepository.findByKnowledgePointIdOrderByCreatedAtDesc(knowledgePointId).stream()
                .filter(Objects::nonNull)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionDTO> getHotQuestions() {
        ensureRankingsWarm();
        List<QuestionDTO> fromRedis = questionRedisService.getHotTop(10, this::loadQuestionById);
        if (!fromRedis.isEmpty()) {
            return fromRedis;
        }
        return questionRepository.findTop10ByOrderByLikeCountDesc().stream()
                .filter(Objects::nonNull)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionDTO> getRecommendedQuestions() {
        ensureRankingsWarm();
        List<QuestionDTO> fromRedis = questionRedisService.getMostViewedTop(10, this::loadQuestionById);
        if (!fromRedis.isEmpty()) {
            return fromRedis;
        }
        return questionRepository.findTop10ByOrderByViewCountDesc().stream()
                .filter(Objects::nonNull)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private void ensureRankingsWarm() {
        if (questionRedisService.isRankingEmpty()) {
            questionRedisService.rebuildRankings(questionRepository.findAll());
        }
    }

    private QuestionDTO loadQuestionById(String id) {
        QuestionDTO cached = questionRedisService.getCachedQuestion(id);
        if (cached != null) {
            return cached;
        }
        return questionRepository.findById(id).map(this::convertToDTO).orElse(null);
    }

    @Override
    public long getQuestionCount() {
        return questionRepository.count();
    }

    @Override
    public void incrementAnswerCount(String questionId) {
        if (questionId == null || questionId.isEmpty()) {
            return;
        }
        Optional<Question> question = questionRepository.findById(questionId);
        question.ifPresent(q -> {
            q.setAnswerCount(q.getAnswerCount() + 1);
            Question saved = questionRepository.save(q);
            questionRedisService.onAnswerCountIncremented(saved);
        });
    }

    @Override
    public List<QuestionDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAll();
        }
        String trimmed = keyword.trim();
        return questionRepository.searchByKeyword(trimmed).stream()
                .filter(Objects::nonNull)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void incrementLikeCount(String questionId) {
        if (questionId == null || questionId.isEmpty()) {
            return;
        }
        Optional<Question> question = questionRepository.findById(questionId);
        question.ifPresent(q -> {
            q.setLikeCount(q.getLikeCount() + 1);
            Question saved = questionRepository.save(q);
            questionRedisService.onLikeIncremented(saved);
        });
    }

    private QuestionDTO convertToDTO(Question entity) {
        if (entity == null) {
            return null;
        }
        return QuestionDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .knowledgePointId(entity.getKnowledgePointId())
                .author(entity.getAuthor())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .answerCount(entity.getAnswerCount())
                .build();
    }

    private Question convertToEntity(QuestionDTO dto) {
        if (dto == null) {
            return null;
        }
        return Question.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .content(dto.getContent())
                .knowledgePointId(dto.getKnowledgePointId())
                .author(dto.getAuthor())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .viewCount(dto.getViewCount())
                .likeCount(dto.getLikeCount())
                .answerCount(dto.getAnswerCount())
                .build();
    }
}
