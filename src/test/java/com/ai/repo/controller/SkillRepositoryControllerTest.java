package com.ai.repo.controller;

import com.ai.repo.dto.LoginResponse;
import com.ai.repo.dto.SkillRatingRequest;
import com.ai.repo.dto.SkillRatingResponse;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.RepoRatingService;
import com.ai.repo.service.SkillRepositoryService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {SkillRepositoryController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class SkillRepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkillRepositoryService skillRepositoryService;

    @MockBean
    private RepoRatingService repoRatingService;

    private RequestPostProcessor withUserId(Long userId) {
        return request -> {
            request.setAttribute("userId", userId);
            return request;
        };
    }

    private RequestPostProcessor withAgentId(Long agentId) {
        return request -> {
            request.setAttribute("agentId", agentId);
            return request;
        };
    }

    private SkillRepository createRepo(Long id, Long agentId) {
        SkillRepository repo = new SkillRepository();
        repo.setId(id);
        repo.setAgentId(agentId);
        repo.setSkillName("test-repo");
        repo.setVersion("1.0");
        repo.setIsPublic(true);
        return repo;
    }

    @Test
    void getById_shouldReturnRepo() throws Exception {
        when(skillRepositoryService.findById(1L)).thenReturn(createRepo(1L, 1L));
        mockMvc.perform(get("/api/skill-repos/1").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getByAgentId_shouldReturnRepos() throws Exception {
        when(skillRepositoryService.findByAgentId(1L)).thenReturn(List.of(createRepo(1L, 1L)));
        mockMvc.perform(get("/api/skill-repos/agent/1").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void forkRepository_shouldReturnForkedRepo() throws Exception {
        SkillRepository forked = createRepo(2L, 2L);
        forked.setParentId(1L);
        when(skillRepositoryService.forkRepository(2L, null, 1L)).thenReturn(forked);

        mockMvc.perform(post("/api/skill-repos/1/fork").with(withAgentId(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void getFileTree_shouldReturnPaths() throws Exception {
        when(skillRepositoryService.getFileTree(1L)).thenReturn(List.of("manifest.json", "skill.md"));
        mockMvc.perform(get("/api/skill-repos/1/tree").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("manifest.json"));
    }

    @Test
    void getFileContent_shouldReturnContent() throws Exception {
        when(skillRepositoryService.getFileContent(1L, "manifest.json")).thenReturn("{\"name\":\"test\"}");
        mockMvc.perform(get("/api/skill-repos/1/file?path=manifest.json").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("{\"name\":\"test\"}"));
    }

    @Test
    void setVisibility_shouldSucceed() throws Exception {
        doNothing().when(skillRepositoryService).setVisibility(1L, 1L, true);
        mockMvc.perform(patch("/api/skill-repos/1/visibility?isPublic=true").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getPublicRepos_shouldReturnList() throws Exception {
        when(skillRepositoryService.findPublicRepos()).thenReturn(List.of(createRepo(1L, 1L)));
        mockMvc.perform(get("/api/skill-repos/public").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void searchRepositories_shouldReturnResults() throws Exception {
        when(skillRepositoryService.searchByKeyword("test")).thenReturn(List.of(createRepo(1L, 1L)));
        mockMvc.perform(get("/api/skill-repos/search?q=test").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getByCategory_shouldReturnRepos() throws Exception {
        when(skillRepositoryService.findByCategory("tools")).thenReturn(List.of(createRepo(1L, 1L)));
        mockMvc.perform(get("/api/skill-repos/category/tools").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getByType_shouldReturnRepos() throws Exception {
        when(skillRepositoryService.findByType("agent")).thenReturn(List.of(createRepo(1L, 1L)));
        mockMvc.perform(get("/api/skill-repos/type/agent").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void incrementDownloadCount_shouldSucceed() throws Exception {
        doNothing().when(skillRepositoryService).incrementDownloadCount(1L);
        mockMvc.perform(post("/api/skill-repos/1/download").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void incrementLikeCount_shouldSucceed() throws Exception {
        doNothing().when(skillRepositoryService).incrementLikeCount(1L);
        mockMvc.perform(post("/api/skill-repos/1/like").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getForks_shouldReturnList() throws Exception {
        when(skillRepositoryService.findById(1L)).thenReturn(createRepo(1L, 1L));
        when(skillRepositoryService.findForksByParentId(1L)).thenReturn(List.of(createRepo(2L, 2L)));
        mockMvc.perform(get("/api/skill-repos/1/forks").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getForks_shouldReturnEmpty_whenNoForks() throws Exception {
        when(skillRepositoryService.findById(1L)).thenReturn(createRepo(1L, 1L));
        when(skillRepositoryService.findForksByParentId(1L)).thenReturn(List.of());
        mockMvc.perform(get("/api/skill-repos/1/forks").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void updateMetadata_shouldSucceed() throws Exception {
        SkillRepository updates = createRepo(1L, 1L);
        updates.setVersion("2.0");
        updates.setDescription("Updated");
        when(skillRepositoryService.updateMetadata(any())).thenReturn(updates);
        mockMvc.perform(put("/api/skill-repos/1")
                        .with(withAgentId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.version").value("2.0"));
    }

    @Test
    void getPublicReposByAgentId_shouldReturnList() throws Exception {
        when(skillRepositoryService.findPublicReposByAgentId(1L)).thenReturn(List.of(createRepo(1L, 1L)));
        mockMvc.perform(get("/api/skill-repos/agent/1/public").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void getPublicReposByAgentId_shouldReturnEmpty_whenNone() throws Exception {
        when(skillRepositoryService.findPublicReposByAgentId(99L)).thenReturn(List.of());
        mockMvc.perform(get("/api/skill-repos/agent/99/public").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void rateRepository_shouldSucceed() throws Exception {
        com.ai.repo.dto.SkillRatingResponse response = new com.ai.repo.dto.SkillRatingResponse();
        response.setSkillId(1L);
        response.setRating(4);
        when(repoRatingService.rate(any(), eq(2L))).thenReturn(response);

        com.ai.repo.dto.SkillRatingRequest req = new com.ai.repo.dto.SkillRatingRequest();
        req.setSkillId(1L);
        req.setRating(4);
        mockMvc.perform(post("/api/skill-repos/1/ratings")
                        .with(withAgentId(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.rating").value(4));
    }

    @Test
    void getRatingSummary_shouldReturnSummary() throws Exception {
        com.ai.repo.dto.SkillRatingAverageResponse summary = new com.ai.repo.dto.SkillRatingAverageResponse();
        summary.setSkillId(1L);
        summary.setAverageRating(4.5);
        summary.setTotalRatings(2);
        when(repoRatingService.getAverageByRepoId(1L)).thenReturn(summary);
        mockMvc.perform(get("/api/skill-repos/1/ratings/summary").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.averageRating").value(4.5));
    }

    @Test
    void getRatings_shouldReturnList() throws Exception {
        com.ai.repo.dto.SkillRatingResponse r = new com.ai.repo.dto.SkillRatingResponse();
        r.setSkillId(1L);
        r.setRating(5);
        r.setRaterAgentName("AgentX");
        when(repoRatingService.getRatingsByRepoId(1L)).thenReturn(List.of(r));
        mockMvc.perform(get("/api/skill-repos/1/ratings").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].raterAgentName").value("AgentX"));
    }

    @Test
    void getMyRatings_shouldReturnList() throws Exception {
        com.ai.repo.dto.SkillRatingResponse r = new com.ai.repo.dto.SkillRatingResponse();
        r.setSkillId(1L);
        r.setRating(3);
        when(repoRatingService.getRatingsByAgentId(2L)).thenReturn(List.of(r));
        mockMvc.perform(get("/api/skill-repos/ratings/my").with(withAgentId(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].rating").value(3));
    }

    @Test
    void getMyRatings_shouldReturn403_whenNoAgent() throws Exception {
        mockMvc.perform(get("/api/skill-repos/ratings/my").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }
}
