package com.jellystudy.evaluate.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.common.entity.AnswerEvaluationDTO;
import com.jellystudy.common.entity.QuestionEvaluationDTO;
import com.jellystudy.evaluate.entity.AnswerEvaluation;
import com.jellystudy.evaluate.entity.QuestionEvaluation;

import java.util.Collections;
import java.util.List;

/**
 * 评估实体与 DTO 转换（JSON 文本字段解析为列表）
 */
public final class EvaluationMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EvaluationMapper() {
    }

    public static QuestionEvaluationDTO toQuestionDto(QuestionEvaluation entity) {
        if (entity == null) {
            return null;
        }
        return QuestionEvaluationDTO.builder()
                .id(entity.getId())
                .questionId(entity.getQuestionId())
                .questionTitle(entity.getQuestionTitle())
                .questionContent(entity.getQuestionContent())
                .knowledgePoints(parseStringList(entity.getKnowledgePoints()))
                .difficulty(entity.getDifficulty())
                .difficultyDescription(entity.getDifficultyDescription())
                .evaluationDetails(entity.getEvaluationDetails())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static AnswerEvaluationDTO toAnswerDto(AnswerEvaluation entity) {
        if (entity == null) {
            return null;
        }
        return AnswerEvaluationDTO.builder()
                .id(entity.getId())
                .answerId(entity.getAnswerId())
                .questionId(entity.getQuestionId())
                .answerContent(entity.getAnswerContent())
                .score(entity.getScore())
                .grade(entity.getGrade())
                .evaluationDetails(entity.getEvaluationDetails())
                .strengths(parseStringList(entity.getStrengths()))
                .suggestions(parseStringList(entity.getSuggestions()))
                .referenceAnswer(entity.getReferenceAnswer())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    static List<String> parseStringList(String jsonOrText) {
        if (jsonOrText == null || jsonOrText.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = jsonOrText.trim();
        if (trimmed.startsWith("[")) {
            try {
                return MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
            } catch (Exception ignored) {
                // fall through
            }
        }
        return List.of(trimmed);
    }
}
