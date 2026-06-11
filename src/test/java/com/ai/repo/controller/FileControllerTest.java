package com.ai.repo.controller;

import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.mapper.FileUploadLogMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {FileController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileUploadLogMapper fileUploadLogMapper;

    private RequestPostProcessor withUserId(Long userId) {
        return request -> {
            request.setAttribute("userId", userId);
            return request;
        };
    }

    @Test
    void getFilesByAgent_shouldReturnFiles_whenValidType() throws Exception {
        FileUploadLog file = new FileUploadLog();
        file.setId(1L);
        file.setAgentId(1L);
        file.setFileType("skill");

        when(fileUploadLogMapper.selectByAgentIdAndFileType(1L, "skill"))
                .thenReturn(List.of(file));

        mockMvc.perform(get("/api/files/skill/agent/1")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void getFilesByAgent_shouldReturn400_whenInvalidType() throws Exception {
        mockMvc.perform(get("/api/files/invalid/agent/1")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid file type. Must be 'skill' or 'memory'"));
    }

    @Test
    void getFileStats_shouldReturnStats() throws Exception {
        when(fileUploadLogMapper.countByAgentId(1L, "skill")).thenReturn(3L);
        when(fileUploadLogMapper.countByAgentId(1L, "memory")).thenReturn(2L);

        mockMvc.perform(get("/api/files/agent/1/stats")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.skillFileCount").value(3))
                .andExpect(jsonPath("$.data.memoryFileCount").value(2))
                .andExpect(jsonPath("$.data.totalFileCount").value(5));
    }
}
