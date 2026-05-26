package com.jellystudy.common.service;

import com.jellystudy.common.entity.KnowledgePointDTO;

import java.util.List;

/**
 * 知识点Dubbo服务接口
 */
public interface IKnowledgePointService {

    List<KnowledgePointDTO> getAll();

    KnowledgePointDTO getById(String id);

    KnowledgePointDTO create(KnowledgePointDTO knowledgePoint);

    KnowledgePointDTO update(String id, KnowledgePointDTO knowledgePoint);

    void delete(String id);
}
