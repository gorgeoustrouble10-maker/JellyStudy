package com.jellystudy.evaluate.controller;

import com.jellystudy.common.entity.AnswerEvaluationDTO;
import com.jellystudy.common.entity.EvaluateAnswerRequest;
import com.jellystudy.common.entity.EvaluateQuestionRequest;
import com.jellystudy.common.entity.QuestionEvaluationDTO;
import jakarta.validation.Valid;
import com.jellystudy.evaluate.config.EvaluateInstanceProperties;
import com.jellystudy.evaluate.config.EvaluateModelProperties;
import com.jellystudy.evaluate.service.EvaluateServiceImpl;
import com.jellystudy.evaluate.entity.AnswerEvaluation;
import com.jellystudy.evaluate.entity.QuestionEvaluation;
import com.jellystudy.evaluate.exception.ApiResponse;
import com.jellystudy.evaluate.redis.EvaluationRedisCache;
import com.jellystudy.evaluate.repository.AnswerEvaluationRepository;
import com.jellystudy.evaluate.repository.QuestionEvaluationRepository;
import com.jellystudy.evaluate.util.EvaluationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 评估服务REST控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Tag(name = "评估服务", description = "大模型问答评估接口")
public class EvaluationController {

    private final EvaluateServiceImpl evaluateService;
    private final QuestionEvaluationRepository questionEvaluationRepository;
    private final AnswerEvaluationRepository answerEvaluationRepository;
    private final EvaluationRedisCache evaluationRedisCache;
    private final EvaluateModelProperties modelProperties;
    private final EvaluateInstanceProperties instanceProperties;

    @Value("${server.port}")
    private int serverPort;

    /**
     * 实例信息（Docker 双实例 + Nacos 配置验证，第十二周作业一）
     */
    @GetMapping("/instance-info")
    @Operation(summary = "实例信息", description = "返回 instanceId、端口、Nacos 中的模型配置")
    public ResponseEntity<Map<String, Object>> instanceInfo() {
        return ResponseEntity.ok(Map.of(
                "instanceId", instanceProperties.getId(),
                "serverPort", serverPort,
                "application", "jellystudy-evaluate-service",
                "modelType", modelProperties.getType(),
                "modelName", modelProperties.getModelName(),
                "timeoutMs", modelProperties.getTimeout(),
                "retryCount", modelProperties.getRetryCount(),
                "configSource", "Nacos evaluate.model.* + 本地 application.yml"));
    }

    /**
     * 评估问题（Dubbo调用）
     */
    @PostMapping("/question")
    @Operation(summary = "评估问题", description = "基于大模型提取知识点并进行难度分级")
    public ResponseEntity<QuestionEvaluationDTO> evaluateQuestion(
            @Valid @RequestBody EvaluateQuestionRequest request) {
        log.info("REST接口调用评估问题, questionId: {}", request.getQuestionId());
        QuestionEvaluationDTO result = evaluateService.evaluateQuestion(
                request.getQuestionId(), request.getQuestionTitle(), request.getQuestionContent());
        return ResponseEntity.ok(result);
    }

    /**
     * 评估答案（Dubbo调用）
     */
    @PostMapping("/answer")
    @Operation(summary = "评估答案", description = "基于大模型对答案进行100分制打分")
    public ResponseEntity<AnswerEvaluationDTO> evaluateAnswer(
            @Valid @RequestBody EvaluateAnswerRequest request) {
        log.info("REST接口调用评估答案, answerId: {}", request.getAnswerId());
        AnswerEvaluationDTO result = evaluateService.evaluateAnswer(
                request.getAnswerId(), request.getQuestionId(),
                request.getQuestionContent(), request.getAnswerContent(), request.getUserId());
        return ResponseEntity.ok(result);
    }

    /**
     * 根据问题ID查询问题评估记录
     */
    @GetMapping("/questions/{questionId}")
    @Operation(summary = "查询问题评估记录", description = "根据问题ID查询评估结果")
    public ResponseEntity<ApiResponse<QuestionEvaluationDTO>> getQuestionEvaluation(
            @Parameter(description = "问题ID") @PathVariable String questionId) {
        Optional<QuestionEvaluation> evaluation = evaluationRedisCache.getQuestionEvaluation(
                questionId, () -> questionEvaluationRepository.findByQuestionId(questionId));
        return evaluation.map(e -> ResponseEntity.ok(ApiResponse.success(EvaluationMapper.toQuestionDto(e))))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据答案ID查询答案评估记录
     */
    @GetMapping("/answers/{answerId}")
    @Operation(summary = "查询答案评估记录", description = "根据答案ID查询评估结果")
    public ResponseEntity<ApiResponse<AnswerEvaluationDTO>> getAnswerEvaluation(
            @Parameter(description = "答案ID") @PathVariable String answerId) {
        Optional<AnswerEvaluation> evaluation = evaluationRedisCache.getAnswerEvaluation(
                answerId, () -> answerEvaluationRepository.findByAnswerId(answerId));
        return evaluation.map(e -> ResponseEntity.ok(ApiResponse.success(EvaluationMapper.toAnswerDto(e))))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 查询所有问题评估记录
     */
    @GetMapping("/questions")
    @Operation(summary = "查询所有问题评估记录", description = "获取所有问题评估列表")
    public ResponseEntity<ApiResponse<List<QuestionEvaluationDTO>>> getAllQuestionEvaluations() {
        List<QuestionEvaluation> evaluations = questionEvaluationRepository.findAll();
        evaluationRedisCache.cacheAllQuestionEvaluations(evaluations);
        List<QuestionEvaluationDTO> dtos = evaluations.stream()
                .map(EvaluationMapper::toQuestionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * 查询所有答案评估记录
     */
    @GetMapping("/answers")
    @Operation(summary = "查询所有答案评估记录", description = "获取所有答案评估列表")
    public ResponseEntity<ApiResponse<List<AnswerEvaluationDTO>>> getAllAnswerEvaluations() {
        List<AnswerEvaluation> evaluations = answerEvaluationRepository.findAll();
        List<AnswerEvaluationDTO> dtos = evaluations.stream()
                .map(EvaluationMapper::toAnswerDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * 根据难度等级查询问题评估记录
     */
    @GetMapping("/questions/difficulty/{difficulty}")
    @Operation(summary = "按难度查询问题评估", description = "根据难度等级查询问题评估记录")
    public ResponseEntity<ApiResponse<List<QuestionEvaluationDTO>>> getQuestionEvaluationsByDifficulty(
            @Parameter(description = "难度等级：EASY/MEDIUM/HARD") @PathVariable String difficulty) {
        List<QuestionEvaluationDTO> dtos = questionEvaluationRepository.findByDifficulty(difficulty.toUpperCase())
                .stream()
                .map(EvaluationMapper::toQuestionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * 根据评分等级查询答案评估记录
     */
    @GetMapping("/answers/grade/{grade}")
    @Operation(summary = "按评分等级查询答案评估", description = "根据评分等级查询答案评估记录")
    public ResponseEntity<ApiResponse<List<AnswerEvaluationDTO>>> getAnswerEvaluationsByGrade(
            @Parameter(description = "评分等级：A/B/C/D") @PathVariable String grade) {
        List<AnswerEvaluationDTO> dtos = answerEvaluationRepository.findByGrade(grade.toUpperCase())
                .stream()
                .map(EvaluationMapper::toAnswerDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * 按问题 ID 删除关联评估（问题评估 + 该问题下全部答案评估）
     */
    @DeleteMapping("/questions/by-question/{questionId}")
    @Operation(summary = "删除问题关联评估", description = "删除问题时级联清理评估记录")
    public ResponseEntity<ApiResponse<Void>> deleteEvaluationsByQuestionId(
            @Parameter(description = "问题ID") @PathVariable String questionId) {
        evaluateService.deleteEvaluationsForQuestion(questionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
