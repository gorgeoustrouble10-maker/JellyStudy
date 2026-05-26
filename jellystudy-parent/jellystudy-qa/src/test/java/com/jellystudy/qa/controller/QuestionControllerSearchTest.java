package com.jellystudy.qa.controller;

import com.jellystudy.common.entity.QuestionDTO;
import com.jellystudy.qa.service.QuestionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuestionController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionControllerSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionServiceImpl questionService;

    @Test
    void searchReturnsResults() throws Exception {
        QuestionDTO dto = QuestionDTO.builder().id("q1").title("Redis").content("缓存").build();
        when(questionService.search(eq("redis"))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/questions/search").param("keyword", "redis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Redis"));
    }
}
