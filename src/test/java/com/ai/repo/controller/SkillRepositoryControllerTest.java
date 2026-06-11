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
}
