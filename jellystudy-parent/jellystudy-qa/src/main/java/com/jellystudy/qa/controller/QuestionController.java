package com.jellystudy.qa.controller;

import com.jellystudy.common.auth.JellystudyUserAttributes;
import com.jellystudy.common.entity.QuestionDTO;
import com.jellystudy.qa.config.JellystudyRedisProperties;
import com.jellystudy.common.api.ApiResponse;
import com.jellystudy.qa.service.QuestionServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 问题 HTTP 接口（本进程 ServiceImpl；跨服务见 {@code @DubboReference}）
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionServiceImpl questionService;
    private final JellystudyRedisProperties redisProperties;

    public QuestionController(QuestionServiceImpl questionService,
                              JellystudyRedisProperties redisProperties) {
        this.questionService = questionService;
        this.redisProperties = redisProperties;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> runtimeConfig() {
        return ResponseEntity.ok(Map.of(
                "recentWindowDays", redisProperties.getRecentWindowDays(),
                "questionCacheTtlMinutes", redisProperties.getQuestionCacheTtlMinutes(),
                "hotKey", redisProperties.getHotKey(),
                "viewRankKey", redisProperties.getViewRankKey(),
                "source", "Nacos jellystudy.redis.*（@RefreshScope 热更新）"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> getAllQuestions() {
        List<QuestionDTO> questions = questionService.getAll();
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDTO>> getQuestionById(@PathVariable String id) {
        QuestionDTO question = questionService.getById(id);
        if (question != null) {
            return ResponseEntity.ok(ApiResponse.success(question));
        }
        return ResponseEntity.status(404).body(ApiResponse.notFound("问题不存在"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuestionDTO>> createQuestion(
            @Valid @RequestBody QuestionDTO questionDTO,
            @RequestAttribute(JellystudyUserAttributes.USER_ID) String userId) {
        questionDTO.setAuthor(userId);
        QuestionDTO created = questionService.create(questionDTO);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDTO>> updateQuestion(@PathVariable String id,
                                                                   @Valid @RequestBody QuestionDTO questionDTO) {
        QuestionDTO updated = questionService.update(id, questionDTO);
        if (updated != null) {
            return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
        }
        return ResponseEntity.status(404).body(ApiResponse.notFound("问题不存在"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable String id) {
        questionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping("/knowledge-point/{knowledgePointId}")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> getQuestionsByKnowledgePoint(@PathVariable String knowledgePointId) {
        List<QuestionDTO> questions = questionService.getByKnowledgePointId(knowledgePointId);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> getHotQuestions() {
        List<QuestionDTO> questions = questionService.getHotQuestions();
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> getRecommendedQuestions() {
        List<QuestionDTO> questions = questionService.getRecommendedQuestions();
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getQuestionCount() {
        long count = questionService.getQuestionCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likeQuestion(@PathVariable String id) {
        questionService.incrementLikeCount(id);
        return ResponseEntity.ok(ApiResponse.success("点赞成功", null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> searchQuestions(@RequestParam String keyword) {
        List<QuestionDTO> results = questionService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
