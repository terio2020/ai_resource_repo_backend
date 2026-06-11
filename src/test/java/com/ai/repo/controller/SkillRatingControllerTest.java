package com.ai.repo.controller;

import com.ai.repo.dto.SkillRatingAverageResponse;
import com.ai.repo.dto.SkillRatingRequest;
import com.ai.repo.dto.SkillRatingResponse;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.SkillRatingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {SkillRatingController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class SkillRatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkillRatingService skillRatingService;

    private RequestPostProcessor withAgentId(Long agentId) {
        return request -> {
            request.setAttribute("agentId", agentId);
            return request;
        };
    }

    private RequestPostProcessor withUserId(Long userId) {
        return request -> {
            request.setAttribute("userId", userId);
            return request;
        };
    }

    @Test
    void rateSkill_shouldReturnRating_whenAgent() throws Exception {
        SkillRatingRequest req = new SkillRatingRequest();
        req.setSkillId(1L);
        req.setRating(4);

        SkillRatingResponse resp = new SkillRatingResponse();
        resp.setSkillId(1L);
        resp.setRating(4);

        when(skillRatingService.rate(any(), anyLong())).thenReturn(resp);

        mockMvc.perform(post("/api/skill-ratings")
                        .with(withAgentId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getSkillAverageRating_shouldReturnAverage() throws Exception {
        SkillRatingAverageResponse resp = new SkillRatingAverageResponse();
        resp.setSkillId(1L);
        resp.setAverageRating(4.5);
        resp.setTotalRatings(10);
        resp.setDistribution(Map.of(1, 0, 2, 0, 3, 1, 4, 4, 5, 5));

        when(skillRatingService.getAverageBySkillId(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/skills/1/rating").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.averageRating").value(4.5));
    }

    @Test
    void getSkillRatings_shouldReturnList() throws Exception {
        SkillRatingResponse resp = new SkillRatingResponse();
        resp.setSkillId(1L);
        resp.setRating(4);

        when(skillRatingService.getRatingsBySkillId(1L)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/skills/1/ratings").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].skillId").value(1));
    }

    @Test
    void getMyRatings_shouldReturnRatings() throws Exception {
        SkillRatingResponse resp = new SkillRatingResponse();
        resp.setSkillId(1L);
        resp.setRating(4);

        when(skillRatingService.getRatingsByAgentId(1L)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/skill-ratings/my").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
