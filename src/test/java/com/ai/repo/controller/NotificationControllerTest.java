package com.ai.repo.controller;

import com.ai.repo.entity.Notification;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.NotificationService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {NotificationController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private RequestPostProcessor withAgentId(Long agentId) {
        return request -> {
            request.setAttribute("agentId", agentId);
            return request;
        };
    }

    private Notification createNotification(Long id, Long agentId) {
        Notification n = new Notification();
        n.setId(id);
        n.setAgentId(agentId);
        n.setTitle("Test");
        n.setContent("Content");
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }

    @Test
    void getNotificationById_shouldReturnNotification_whenOwned() throws Exception {
        when(notificationService.findById(1L)).thenReturn(createNotification(1L, 1L));
        mockMvc.perform(get("/api/notifications/1").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getNotificationById_shouldReturn404_whenNotFound() throws Exception {
        when(notificationService.findById(999L)).thenReturn(null);
        mockMvc.perform(get("/api/notifications/999").with(withAgentId(1L)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getNotificationById_shouldReturn403_whenNotOwned() throws Exception {
        when(notificationService.findById(1L)).thenReturn(createNotification(1L, 2L));
        mockMvc.perform(get("/api/notifications/1").with(withAgentId(1L)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void getNotifications_shouldReturnAll() throws Exception {
        when(notificationService.findByAgentId(1L)).thenReturn(List.of(createNotification(1L, 1L)));
        mockMvc.perform(get("/api/notifications").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getNotifications_shouldReturnUnread_whenFiltered() throws Exception {
        when(notificationService.findUnreadByAgentId(1L)).thenReturn(List.of(createNotification(1L, 1L)));
        mockMvc.perform(get("/api/notifications?unread=true").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getUnreadCount_shouldReturnCount() throws Exception {
        when(notificationService.countUnreadByAgentId(1L)).thenReturn(5L);
        mockMvc.perform(get("/api/notifications/count/unread").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void markAsRead_shouldSucceed_whenOwned() throws Exception {
        when(notificationService.findById(1L)).thenReturn(createNotification(1L, 1L));
        when(notificationService.markAsRead(1L)).thenReturn(true);
        mockMvc.perform(post("/api/notifications/1/read").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void markAllAsRead_shouldSucceed() throws Exception {
        when(notificationService.markAllAsRead(1L)).thenReturn(true);
        mockMvc.perform(post("/api/notifications/read-all").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteNotification_shouldSucceed_whenOwned() throws Exception {
        when(notificationService.findById(1L)).thenReturn(createNotification(1L, 1L));
        when(notificationService.delete(1L)).thenReturn(true);
        mockMvc.perform(delete("/api/notifications/1").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
