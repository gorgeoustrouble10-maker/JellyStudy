package com.jellystudy.knowledge.service;

import com.jellystudy.common.entity.KnowledgePointDTO;
import com.jellystudy.knowledge.config.KnowledgeListProperties;
import com.jellystudy.knowledge.entity.KnowledgePoint;
import com.jellystudy.knowledge.repository.KnowledgePointRepository;
import com.jellystudy.knowledge.repository.QuestionLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgePointServiceImplTest {

    @Mock
    private KnowledgePointRepository knowledgePointRepository;

    @Mock
    private QuestionLinkRepository questionLinkRepository;

    @Mock
    private KnowledgeListProperties listProperties;

    @InjectMocks
    private KnowledgePointServiceImpl service;

    @Test
    void getAllTruncatesByNacosConfiguredMaxSize() {
        KnowledgePoint k1 = KnowledgePoint.builder().id("k1").name("Redis").build();
        KnowledgePoint k2 = KnowledgePoint.builder().id("k2").name("Dubbo").build();
        when(knowledgePointRepository.findAll()).thenReturn(List.of(k1, k2));
        when(questionLinkRepository.countByKnowledgePointId("k1")).thenReturn(3L);
        when(questionLinkRepository.countByKnowledgePointId("k2")).thenReturn(5L);
        when(listProperties.getMaxListSize()).thenReturn(1);

        List<KnowledgePointDTO> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("k1", result.get(0).getId());
        assertEquals(3, result.get(0).getQuestionCount());
    }

    @Test
    void getAllReturnsAllWhenMaxSizeIsZero() {
        KnowledgePoint k1 = KnowledgePoint.builder().id("k1").name("Redis").build();
        KnowledgePoint k2 = KnowledgePoint.builder().id("k2").name("Dubbo").build();
        when(knowledgePointRepository.findAll()).thenReturn(List.of(k1, k2));
        when(questionLinkRepository.countByKnowledgePointId("k1")).thenReturn(3L);
        when(questionLinkRepository.countByKnowledgePointId("k2")).thenReturn(5L);
        when(listProperties.getMaxListSize()).thenReturn(0);

        List<KnowledgePointDTO> result = service.getAll();

        assertEquals(2, result.size());
        assertEquals("k2", result.get(1).getId());
        assertEquals(5, result.get(1).getQuestionCount());
    }
}
