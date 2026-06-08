package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.BatchDeleteRequest;
import com.ai.repo.dto.FileUploadResponse;
import com.ai.repo.dto.SkillCreateRequest;
import com.ai.repo.dto.SkillUpdateRequest;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.entity.Skill;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.FileStorageService;
import com.ai.repo.service.ShareService;
import com.ai.repo.service.SkillService;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {SkillController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkillService skillService;

    @MockBean
    private AgentService agentService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private ShareService shareService;

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

    private RequestPostProcessor withUserAndAgent(Long userId, Long agentId) {
        return request -> {
            request.setAttribute("userId", userId);
            request.setAttribute("agentId", agentId);
            return request;
        };
    }

    private Skill createSampleSkill(Long id) {
        Skill s = new Skill();
        s.setId(id);
        s.setUserId(1L);
        s.setAgentId(1L);
        s.setName("test-skill");
        s.setVersion("1.0");
        s.setDescription("desc");
        s.setCategory("java");
        s.setIsPublic(true);
        s.setDownloadCount(0);
        s.setLikeCount(0);
        return s;
    }

    // ==================== CREATE ====================

    @Test
    void createSkill_shouldReturn201() throws Exception {
        SkillCreateRequest request = new SkillCreateRequest();
        request.setName("new-skill");
        request.setVersion("1.0");
        request.setDescription("desc");
        request.setCategory("java");
        request.setIsPublic(true);

        Skill created = createSampleSkill(1L);
        when(skillService.upsert(any(Skill.class))).thenReturn(created);

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withUserAndAgent(1L, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("test-skill"));
    }

    // ==================== UPDATE ====================

    @Test
    void updateSkill_shouldReturnUpdated() throws Exception {
        SkillUpdateRequest request = new SkillUpdateRequest();
        request.setName("updated-skill");
        request.setVersion("2.0");
        request.setDescription("updated");
        request.setCategory("python");
        request.setIsPublic(false);

        Skill updated = createSampleSkill(1L);
        updated.setName("updated-skill");
        when(skillService.update(any(Skill.class))).thenReturn(updated);

        mockMvc.perform(put("/api/skills/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withUserAndAgent(1L, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("updated-skill"));
    }

    @Test
    void updateSkill_shouldReturn404_whenNotFound() throws Exception {
        SkillUpdateRequest request = new SkillUpdateRequest();
        request.setName("ghost");
        when(skillService.update(any(Skill.class)))
                .thenThrow(new BusinessException(404, "Skill not found"));

        mockMvc.perform(put("/api/skills/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withUserAndAgent(1L, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Skill not found"));
    }

    // ==================== DELETE ====================

    @Test
    void deleteSkill_shouldReturn200() throws Exception {
        when(skillService.delete(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/skills/1")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(skillService).delete(1L);
    }

    // ==================== GET BY ID ====================

    @Test
    void getSkillById_shouldReturnSkill() throws Exception {
        when(skillService.findById(1L)).thenReturn(createSampleSkill(1L));

        mockMvc.perform(get("/api/skills/1")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getSkillById_shouldReturnNull_whenNotFound() throws Exception {
        when(skillService.findById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/skills/999")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== GET BY USER ====================

    @Test
    void getSkillsByUserId_shouldReturnList() throws Exception {
        when(skillService.findByUserId(1L)).thenReturn(List.of(createSampleSkill(1L)));

        mockMvc.perform(get("/api/skills/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    // ==================== GET BY AGENT ====================

    @Test
    void getSkillsByAgentId_shouldMergeDirectAndBound() throws Exception {
        Skill direct = createSampleSkill(1L);
        Skill bound = createSampleSkill(2L);
        bound.setId(2L);
        when(skillService.findByAgentId(1L)).thenReturn(List.of(direct));
        when(agentService.getAgentSkills(1L)).thenReturn(List.of(bound));

        mockMvc.perform(get("/api/skills/agent/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getSkillsByAgentId_shouldDeduplicate() throws Exception {
        Skill skill = createSampleSkill(1L);
        when(skillService.findByAgentId(1L)).thenReturn(List.of(skill, skill));
        when(agentService.getAgentSkills(1L)).thenReturn(List.of(skill));

        mockMvc.perform(get("/api/skills/agent/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ==================== GET BY CATEGORY ====================

    @Test
    void getSkillsByCategory_shouldReturnList() throws Exception {
        when(skillService.findByCategory("java")).thenReturn(List.of(createSampleSkill(1L)));

        mockMvc.perform(get("/api/skills/category/java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].category").value("java"));
    }

    // ==================== GET PUBLIC ====================

    @Test
    void getPublicSkills_shouldReturnList() throws Exception {
        when(skillService.findByPublic(true)).thenReturn(List.of(createSampleSkill(1L)));

        mockMvc.perform(get("/api/skills/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    // ==================== SEARCH ====================

    @Test
    void searchSkills_shouldReturnList() throws Exception {
        when(skillService.searchByKeyword("test")).thenReturn(List.of(createSampleSkill(1L)));

        mockMvc.perform(get("/api/skills/search")
                        .param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("test-skill"));
    }

    // ==================== INCREMENT DOWNLOAD ====================

    @Test
    void incrementDownloadCount_shouldReturn200_whenAgent() throws Exception {
        when(skillService.incrementDownloadCount(1L)).thenReturn(true);

        mockMvc.perform(post("/api/skills/1/download")
                        .with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void incrementDownloadCount_shouldReturn403_whenNotAgent() throws Exception {
        mockMvc.perform(post("/api/skills/1/download")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ==================== INCREMENT LIKE ====================

    @Test
    void incrementLikeCount_shouldReturn200_whenAgent() throws Exception {
        when(skillService.incrementLikeCount(1L)).thenReturn(true);

        mockMvc.perform(post("/api/skills/1/like")
                        .with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void incrementLikeCount_shouldReturn403_whenNotAgent() throws Exception {
        mockMvc.perform(post("/api/skills/1/like")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ==================== SHARE ====================

    @Test
    void shareSkill_shouldReturnShareUrl() throws Exception {
        when(shareService.createShareLink(1L, 1L)).thenReturn("token123");

        mockMvc.perform(post("/api/skills/1/share")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shareToken").value("token123"))
                .andExpect(jsonPath("$.data.shareUrl").value("/api/skills/shared/token123"));
    }

    // ==================== GET SHARED ====================

    @Test
    void getSharedSkill_shouldReturnSkill() throws Exception {
        when(shareService.getSharedSkill("token123")).thenReturn(createSampleSkill(1L));

        mockMvc.perform(get("/api/skills/shared/token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // ==================== BATCH DELETE ====================

    @Test
    void batchDeleteSkills_shouldReturnCount() throws Exception {
        BatchDeleteRequest request = new BatchDeleteRequest();
        request.setIds(List.of(1L, 2L, 3L));
        when(skillService.batchDelete(List.of(1L, 2L, 3L))).thenReturn(3);

        mockMvc.perform(delete("/api/skills/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(3));
    }

    // ==================== UPLOAD FILE ====================

    @Test
    void uploadSkillFile_shouldReturnResponse() throws Exception {
        FileUploadResponse uploadResponse = new FileUploadResponse();
        uploadResponse.setFileName("test.md");
        uploadResponse.setFileSize(100L);
        uploadResponse.setFilePath("skill/test.md");

        MockMultipartFile file = new MockMultipartFile("file", "test.md", "text/markdown", "content".getBytes());
        when(fileStorageService.saveFile(any(), eq(1L), eq(1L), eq("skill"), eq("desc")))
                .thenReturn(uploadResponse);

        mockMvc.perform(multipart("/api/skills/1/upload")
                        .file(file)
                        .param("description", "desc")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("test.md"));
    }

    // ==================== DOWNLOAD FILE ====================

    @Test
    void downloadSkillFile_shouldReturnAttachment() throws Exception {
        ByteArrayResource resource = new ByteArrayResource("content".getBytes());
        when(fileStorageService.loadFileAsResource(1L, 1L)).thenReturn(resource);

        FileUploadLog log = new FileUploadLog();
        log.setId(1L);
        log.setOriginalFileName("test.md");
        when(fileStorageService.getFileUploadLog(1L)).thenReturn(log);

        mockMvc.perform(get("/api/skills/file/1")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test.md\""))
                .andExpect(content().contentType("application/octet-stream"));
    }

    // ==================== GET FILES ====================

    @Test
    void getSkillFiles_shouldReturnList() throws Exception {
        FileUploadLog log = new FileUploadLog();
        log.setId(1L);
        log.setOriginalFileName("test.md");
        when(fileStorageService.getFileList(1L, "skill", null)).thenReturn(List.of(log));

        mockMvc.perform(get("/api/skills/1/files")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].originalFileName").value("test.md"));
    }

    // ==================== DELETE FILE ====================

    @Test
    void deleteSkillFile_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/skills/file/1")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(fileStorageService).deleteFile(1L, 1L);
    }
}
