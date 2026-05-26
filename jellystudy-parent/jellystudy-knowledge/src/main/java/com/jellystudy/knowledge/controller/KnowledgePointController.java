package com.jellystudy.knowledge.controller;

import com.jellystudy.common.entity.KnowledgePointDTO;
import com.jellystudy.knowledge.exception.ApiResponse;
import com.jellystudy.knowledge.service.KnowledgePointServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识点 HTTP 接口（本进程 ServiceImpl；能力通过 @DubboService 注册到 Nacos）
 */
@RestController
@RequestMapping("/api/knowledge-points")
public class KnowledgePointController {

    private final KnowledgePointServiceImpl knowledgePointService;

    public KnowledgePointController(KnowledgePointServiceImpl knowledgePointService) {
        this.knowledgePointService = knowledgePointService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<KnowledgePointDTO>>> getAll() {
        List<KnowledgePointDTO> result = knowledgePointService.getAll();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KnowledgePointDTO>> getById(@PathVariable String id) {
        KnowledgePointDTO knowledgePoint = knowledgePointService.getById(id);
        if (knowledgePoint != null) {
            return ResponseEntity.ok(ApiResponse.success(knowledgePoint));
        }
        return ResponseEntity.status(404).body(ApiResponse.notFound("知识点不存在"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgePointDTO>> create(@Valid @RequestBody KnowledgePointDTO knowledgePoint) {
        KnowledgePointDTO created = knowledgePointService.create(knowledgePoint);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<KnowledgePointDTO>> update(@PathVariable String id, 
                                                                @Valid @RequestBody KnowledgePointDTO knowledgePoint) {
        KnowledgePointDTO updated = knowledgePointService.update(id, knowledgePoint);
        if (updated != null) {
            return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
        }
        return ResponseEntity.status(404).body(ApiResponse.notFound("知识点不存在"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        knowledgePointService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
