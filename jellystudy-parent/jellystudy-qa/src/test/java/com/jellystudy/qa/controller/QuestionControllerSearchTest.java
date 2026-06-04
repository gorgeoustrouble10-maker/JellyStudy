package com.jellystudy.qa.controller;

import com.jellystudy.common.entity.QuestionDTO;
import com.jellystudy.qa.config.JellystudyRedisProperties;
import com.jellystudy.qa.service.QuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionControllerSearchTest {

    private MockMvc mockMvc;

    @Mock
    private QuestionServiceImpl questionService;

    @Mock
    private JellystudyRedisProperties redisProperties;

    @InjectMocks
    private QuestionController questionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(questionController).build();
    }

    @Test
    void searchReturnsResults() throws Exception {
        QuestionDTO dto = QuestionDTO.builder().id("q1").title("Redis").content("缓存").build();
        when(questionService.search(eq("redis"))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/questions/search").param("keyword", "redis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Redis"));
    }

    @Test
    void configEndpointReturnsNacosBackedFields() throws Exception {
        when(redisProperties.getRecentWindowDays()).thenReturn(7);
        when(redisProperties.getQuestionCacheTtlMinutes()).thenReturn(30);
        when(redisProperties.getHotKey()).thenReturn("jelly:hot:questions");
        when(redisProperties.getViewRankKey()).thenReturn("jelly:view:rank");

        mockMvc.perform(get("/api/questions/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentWindowDays").value(7))
                .andExpect(jsonPath("$.questionCacheTtlMinutes").value(30))
                .andExpect(jsonPath("$.hotKey").value("jelly:hot:questions"))
                .andExpect(jsonPath("$.viewRankKey").value("jelly:view:rank"));
    }
}
