package com.ai.repo.controller;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AdminUserResponse;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AdminUserController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        ValidationAutoConfiguration.class
})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @Test
    void listUsers_shouldReturnPage() throws Exception {
        AdminUserResponse item = new AdminUserResponse();
        item.setId(1L);
        item.setUsername("user1");
        item.setRole("USER");
        PageResult<AdminUserResponse> page =
                new PageResult<>(Collections.singletonList(item), 1L, 1L, 10L);
        when(adminUserService.listUsers(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].username").value("user1"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void updateRole_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/admin/users/2/role")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminUserService).updateRole(1L, 2L, "ADMIN");
    }

    @Test
    void updateRole_shouldRejectInvalidRole() throws Exception {
        mockMvc.perform(patch("/api/admin/users/2/role")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPER\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateStatus_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/admin/users/2/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminUserService).updateStatus(1L, 2L, "DISABLED");
    }

    @Test
    void updateStatus_shouldRejectInvalidStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/users/2/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BANNED\"}"))
                .andExpect(status().is4xxClientError());
    }
}
