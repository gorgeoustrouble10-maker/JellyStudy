package com.jellystudy.knowledge.controller;

import com.jellystudy.common.entity.KnowledgePointDTO;
import com.jellystudy.knowledge.config.KnowledgeListProperties;
import com.jellystudy.knowledge.service.KnowledgePointServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KnowledgePointControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KnowledgePointServiceImpl knowledgePointService;

    @Mock
    private KnowledgeListProperties listProperties;

    @InjectMocks
    private KnowledgePointController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void runtimeConfigReturnsNacosBackedFields() throws Exception {
        when(listProperties.getMaxListSize()).thenReturn(3);

        mockMvc.perform(get("/api/knowledge-points/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxListSize").value(3))
                .andExpect(jsonPath("$.source").value("Nacos knowledge.list.max-list-size（@RefreshScope 热更新）"));
    }

    @Test
    void getAllReturnsWrappedSuccessPayload() throws Exception {
        KnowledgePointDTO dto = KnowledgePointDTO.builder()
                .id("kp-1")
                .name("Redis")
                .description("缓存基础")
                .build();
        when(knowledgePointService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/knowledge-points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("kp-1"))
                .andExpect(jsonPath("$.data[0].name").value("Redis"));
    }
}
