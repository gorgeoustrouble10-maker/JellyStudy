package com.jellystudy.coach.service;

import com.jellystudy.common.entity.KnowledgePointDTO;
import com.jellystudy.common.service.IKnowledgePointService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeScopeService {

    @DubboReference(version = "1.0.0", protocol = "tri", check = false, timeout = 10000)
    private IKnowledgePointService knowledgePointService;

    public List<KnowledgePointDTO> loadAll() {
        try {
            List<KnowledgePointDTO> list = knowledgePointService.getAll();
            return list != null ? list : List.of();
        } catch (Exception e) {
            log.warn("Dubbo 调用知识点服务失败，Coach 将使用 MongoDB 缓存的薄弱点", e);
            return List.of();
        }
    }

    public List<String> loadNames() {
        return loadAll().stream().map(KnowledgePointDTO::getName).collect(Collectors.toList());
    }

    public boolean isInScope(String weakPoint, List<String> scope) {
        if (weakPoint == null || scope == null || scope.isEmpty()) {
            return false;
        }
        return scope.stream().anyMatch(k -> k.equals(weakPoint) || k.contains(weakPoint) || weakPoint.contains(k));
    }

    public String buildScopeDescription(List<KnowledgePointDTO> points) {
        if (points.isEmpty()) {
            return "（暂无知识点，请先在「知识点管理」中添加）";
        }
        return points.stream()
                .map(kp -> "- " + kp.getName() + "：" + (kp.getDescription() != null ? kp.getDescription() : ""))
                .collect(Collectors.joining("\n"));
    }
}
