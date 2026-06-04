package com.jellystudy.qa.controller;

import com.jellystudy.common.auth.JellystudyUserAttributes;
import com.jellystudy.common.entity.AnswerDTO;
import com.jellystudy.common.entity.CommentDTO;
import com.jellystudy.common.api.ApiResponse;
import com.jellystudy.qa.service.AnswerServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 回答 HTTP 接口（本进程 ServiceImpl）
 */
@RestController
@RequestMapping("/api/answers")
public class AnswerController {

    private final AnswerServiceImpl answerService;

    public AnswerController(AnswerServiceImpl answerService) {
        this.answerService = answerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AnswerDTO>>> getAllAnswers() {
        List<AnswerDTO> answers = answerService.getAll();
        return ResponseEntity.ok(ApiResponse.success(answers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnswerDTO>> getAnswerById(@PathVariable String id) {
        AnswerDTO answer = answerService.getById(id);
        if (answer != null) {
            return ResponseEntity.ok(ApiResponse.success(answer));
        }
        return ResponseEntity.status(404).body(ApiResponse.notFound("回答不存在"));
    }

    @GetMapping("/question/{questionId}")
    public ResponseEntity<ApiResponse<List<AnswerDTO>>> getAnswersByQuestionId(@PathVariable String questionId) {
        List<AnswerDTO> answers = answerService.getByQuestionId(questionId);
        return ResponseEntity.ok(ApiResponse.success(answers));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnswerDTO>> createAnswer(
            @Valid @RequestBody AnswerDTO answerDTO,
            @RequestAttribute(JellystudyUserAttributes.USER_ID) String userId) {
        answerDTO.setAuthor(userId);
        AnswerDTO created = answerService.create(answerDTO);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnswerDTO>> updateAnswer(@PathVariable String id,
                                                               @Valid @RequestBody AnswerDTO answerDTO) {
        AnswerDTO updated = answerService.update(id, answerDTO);
        if (updated != null) {
            return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
        }
        return ResponseEntity.status(404).body(ApiResponse.notFound("回答不存在"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(@PathVariable String id) {
        answerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PostMapping("/{answerId}/comments")
    public ResponseEntity<ApiResponse<Void>> addComment(@PathVariable String answerId,
                                                        @Valid @RequestBody CommentDTO commentDTO,
                                                        @RequestAttribute(JellystudyUserAttributes.USER_ID) String userId) {
        commentDTO.setAuthor(userId);
        answerService.addComment(answerId, commentDTO);
        return ResponseEntity.ok(ApiResponse.success("评论添加成功", null));
    }

    @DeleteMapping("/{answerId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> removeComment(@PathVariable String answerId,
                                                           @PathVariable String commentId) {
        answerService.removeComment(answerId, commentId);
        return ResponseEntity.ok(ApiResponse.success("评论删除成功", null));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likeAnswer(@PathVariable String id) {
        answerService.incrementLikeCount(id);
        return ResponseEntity.ok(ApiResponse.success("点赞成功", null));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getAnswerCount() {
        long count = answerService.getAnswerCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
