package com.ai.repo.controller;

import com.ai.repo.entity.FileUploadLog;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    // ==================== POST /api/memories — agentId from body ====================

    @Test
    void createMemory_shouldAcceptAgentIdInBody() throws Exception {
        Memory memory = new Memory();
        memory.setId(2L);
        memory.setAgentId(7L);
        memory.setTitle("Default Title");
        when(memoryService.upsert(any(Memory.class))).thenReturn(memory);

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":7,\"content\":\"body\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentId").value(7));
    }

    @Test
    void createMemory_shouldUseDefaultTitleWhenBlank() throws Exception {
        Memory memory = new Memory();
        memory.setId(3L);
        memory.setAgentId(5L);
        memory.setTitle("Memory_<timestamp>");
        when(memoryService.upsert(any(Memory.class))).thenReturn(memory);

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"no title\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(org.hamcrest.Matchers.startsWith("Memory_")));
    }

    @Test
    void createMemory_shouldReturn400_whenNoAgentId() throws Exception {
        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"orphan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createMemory_shouldJoinTagsWithComma() throws Exception {
        Memory memory = new Memory();
        memory.setId(4L);
        memory.setAgentId(5L);
        when(memoryService.upsert(any(Memory.class))).thenReturn(memory);

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":5,\"tags\":[\"alpha\",\"beta\",\"gamma\"]}")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk());

        verify(memoryService).upsert(org.mockito.ArgumentMatchers.argThat(
                m -> "alpha,beta,gamma".equals(m.getTags())));
    }

    // ==================== PUT /api/memories/{id} ====================

    @Test
    void updateMemory_shouldPassIdAndBody() throws Exception {
        Memory memory = new Memory();
        memory.setId(1L);
        memory.setAgentId(5L);
        memory.setTitle("Updated");
        when(memoryService.update(any(Memory.class))).thenReturn(memory);

        mockMvc.perform(put("/api/memories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"content\":\"new content\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    // ==================== DELETE /api/memories/{id} ====================

    @Test
    void deleteMemory_shouldInvokeService() throws Exception {
        mockMvc.perform(delete("/api/memories/1")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(memoryService).delete(1L);
    }

    @Test
    void deleteMemory_notFound_shouldReturnError() throws Exception {
        org.mockito.Mockito.doThrow(new BusinessException("Memory not found"))
                .when(memoryService).delete(999L);

        mockMvc.perform(delete("/api/memories/999")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== GET /api/memories/{id} ====================

    @Test
    void getMemoryById_shouldReturnMemory() throws Exception {
        Memory memory = new Memory();
        memory.setId(1L);
        memory.setTitle("Test");
        when(memoryService.findById(1L)).thenReturn(memory);

        mockMvc.perform(get("/api/memories/1")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getMemoryById_notFound_shouldReturnError() throws Exception {
        when(memoryService.findById(999L))
                .thenThrow(new BusinessException("Memory not found"));

        mockMvc.perform(get("/api/memories/999")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== GET /api/memories/user/{userId} ====================

    @Test
    void getMemoriesByUserId_shouldReturnList() throws Exception {
        when(memoryService.findByUserId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/memories/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/memories/agent/{agentId} ====================

    @Test
    void getMemoriesByAgentId_shouldReturnList() throws Exception {
        when(memoryService.findByAgentId(5L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/memories/agent/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/memories/category/{category} ====================

    @Test
    void getMemoriesByCategory_shouldReturnList() throws Exception {
        when(memoryService.findByCategory("notes")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/memories/category/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/memories/public ====================

    @Test
    void getPublicMemories_shouldReturnList() throws Exception {
        when(memoryService.findByPublic(true)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/memories/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/memories/search ====================

    @Test
    void searchMemories_shouldInvokeSearch() throws Exception {
        when(memoryService.searchByKeyword("kotlin")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/memories/search").param("keyword", "kotlin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(memoryService).searchByKeyword("kotlin");
    }

    // ==================== DELETE /api/memories/batch ====================

    @Test
    void batchDeleteMemories_shouldReturnCount() throws Exception {
        when(memoryService.batchDelete(org.mockito.ArgumentMatchers.anyList())).thenReturn(3);

        mockMvc.perform(delete("/api/memories/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1,2,3]}")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    // ==================== File upload/download ====================

    @Test
    void uploadMemoryFile_shouldReturnResponse() throws Exception {
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "memo.md", "text/markdown", "# hello".getBytes());

        com.ai.repo.dto.FileUploadResponse resp = new com.ai.repo.dto.FileUploadResponse();
        resp.setFileId(11L);
        resp.setFileName("memo.md");
        when(fileStorageService.saveFile(any(), eq(1L), eq(5L), eq("memory"), any())).thenReturn(resp);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/memories/5/upload").file(file)
                        .param("description", "test memo")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileId").value(11));
    }

    @Test
    void downloadMemoryFile_shouldReturnOctetStream() throws Exception {
        FileUploadLog log = new FileUploadLog();
        log.setOriginalFileName("memo.md");
        when(fileStorageService.loadFileAsResource(eq(11L), eq(1L)))
                .thenReturn(new ByteArrayResource("content".getBytes()));
        when(fileStorageService.getFileUploadLog(11L)).thenReturn(log);

        mockMvc.perform(get("/api/memories/file/11")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("memo.md")));
    }

    @Test
    void getMemoryFiles_shouldReturnList() throws Exception {
        when(fileStorageService.getFileList(eq(5L), eq("memory"), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/memories/5/files")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteMemoryFile_shouldInvokeService() throws Exception {
        mockMvc.perform(delete("/api/memories/file/11")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(fileStorageService).deleteFile(eq(11L), eq(1L));
    }
}
