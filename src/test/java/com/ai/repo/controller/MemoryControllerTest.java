package com.ai.repo.controller;

import com.ai.repo.entity.Memory;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.FileStorageService;
import com.ai.repo.service.MemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MemoryController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class
})
class MemoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemoryService memoryService;

    @MockBean
    private FileStorageService fileStorageService;

    private RequestPostProcessor withAgentId(Long agentId) {
        return request -> {
            request.setAttribute("agentId", agentId);
            request.setAttribute("userId", 1L);
            return request;
        };
    }

    // ==================== POST /api/memories (create) ====================

    @Test
    void createMemory_shouldInitializeCounts() throws Exception {
        Memory memory = new Memory();
        memory.setId(1L);
        memory.setAgentId(5L);
        memory.setTitle("Test Memory");
        memory.setDownloadCount(0);
        memory.setLikeCount(0);
        when(memoryService.upsert(any(Memory.class))).thenReturn(memory);

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Memory\",\"content\":\"Some content\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.downloadCount").value(0))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    // ==================== POST /api/memories/{id}/download ====================

    @Test
    void incrementDownloadCount_shouldSucceed() throws Exception {
        when(memoryService.incrementDownloadCount(1L)).thenReturn(true);

        mockMvc.perform(post("/api/memories/1/download")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void incrementDownloadCount_notFound_shouldReturnError() throws Exception {
        when(memoryService.incrementDownloadCount(999L))
                .thenThrow(new BusinessException("Memory not found"));

        mockMvc.perform(post("/api/memories/999/download")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== POST /api/memories/{id}/like ====================

    @Test
    void incrementLikeCount_shouldSucceed() throws Exception {
        when(memoryService.incrementLikeCount(1L)).thenReturn(true);

        mockMvc.perform(post("/api/memories/1/like")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void incrementLikeCount_notFound_shouldReturnError() throws Exception {
        when(memoryService.incrementLikeCount(999L))
                .thenThrow(new BusinessException("Memory not found"));

        mockMvc.perform(post("/api/memories/999/like")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
