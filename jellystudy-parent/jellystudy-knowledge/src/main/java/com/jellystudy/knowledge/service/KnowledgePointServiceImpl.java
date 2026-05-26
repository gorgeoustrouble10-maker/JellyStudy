package com.jellystudy.knowledge.service;

import com.jellystudy.common.entity.KnowledgePointDTO;
import com.jellystudy.common.service.IKnowledgePointService;
import com.jellystudy.knowledge.entity.KnowledgePoint;
import com.jellystudy.knowledge.repository.KnowledgePointRepository;
import com.jellystudy.knowledge.repository.QuestionLinkRepository;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 知识点Dubbo服务实现
 */
@DubboService(version = "1.0.0", protocol = "tri")
public class KnowledgePointServiceImpl implements IKnowledgePointService {

    @Autowired
    private KnowledgePointRepository knowledgePointRepository;

    @Autowired
    private QuestionLinkRepository questionLinkRepository;

    @Override
    public List<KnowledgePointDTO> getAll() {
        return knowledgePointRepository.findAll().stream()
                .filter(Objects::nonNull)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public KnowledgePointDTO getById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        Optional<KnowledgePoint> knowledgePoint = knowledgePointRepository.findById(id);
        return knowledgePoint.map(this::convertToDTO).orElse(null);
    }

    @Override
    public KnowledgePointDTO create(KnowledgePointDTO knowledgePointDTO) {
        if (knowledgePointDTO == null) {
            throw new IllegalArgumentException("知识点DTO不能为空");
        }
        KnowledgePoint knowledgePoint = convertToEntity(knowledgePointDTO);
        if (knowledgePoint.getId() == null || knowledgePoint.getId().isEmpty()) {
            knowledgePoint.setId(UUID.randomUUID().toString());
        }
        knowledgePoint.setCreatedAt(new Date());
        knowledgePoint.setUpdatedAt(new Date());
        KnowledgePoint saved = knowledgePointRepository.save(knowledgePoint);
        return convertToDTO(saved);
    }

    @Override
    public KnowledgePointDTO update(String id, KnowledgePointDTO knowledgePointDTO) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID不能为空");
        }
        if (knowledgePointDTO == null) {
            throw new IllegalArgumentException("知识点DTO不能为空");
        }
        Optional<KnowledgePoint> existing = knowledgePointRepository.findById(id);
        if (existing.isPresent()) {
            KnowledgePoint updated = existing.get();
            updated.setName(knowledgePointDTO.getName());
            updated.setDescription(knowledgePointDTO.getDescription());
            updated.setParentId(knowledgePointDTO.getParentId());
            updated.setPath(knowledgePointDTO.getPath());
            updated.setUpdatedAt(new Date());
            return convertToDTO(knowledgePointRepository.save(updated));
        }
        return null;
    }

    @Override
    public void delete(String id) {
        if (id != null && !id.isEmpty()) {
            knowledgePointRepository.deleteById(id);
        }
    }

    private KnowledgePointDTO convertToDTO(KnowledgePoint entity) {
        if (entity == null) {
            return null;
        }
        long count = questionLinkRepository.countByKnowledgePointId(entity.getId());
        return KnowledgePointDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .parentId(entity.getParentId())
                .path(entity.getPath())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .questionCount((int) count)
                .build();
    }

    private KnowledgePoint convertToEntity(KnowledgePointDTO dto) {
        if (dto == null) {
            return null;
        }
        return KnowledgePoint.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .parentId(dto.getParentId())
                .path(dto.getPath())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
