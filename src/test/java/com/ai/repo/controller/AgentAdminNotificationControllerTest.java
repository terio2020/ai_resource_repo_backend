package com.ai.repo.controller;

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

import com.ai.repo.entity.Agent;
import com.ai.repo.entity.User;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.AdminEmailService;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.UserService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AgentAdminNotificationController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class, ValidationAutoConfiguration.class})
class AgentAdminNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AgentService agentService;
    @MockBean
    private UserService userService;
    @MockBean
    private AdminEmailService adminEmailService;

    @Test
    void administratorOwnedAgentCanEmailItsOwner() throws Exception {
        Agent agent = new Agent();
        agent.setId(7L);
        agent.setUserId(42L);
        agent.setDisplayName("Cline");
        User owner = new User();
        owner.setId(42L);
        owner.setRole("ADMIN");
        owner.setStatus("ACTIVE");
        when(agentService.findById(7L)).thenReturn(agent);
        when(userService.findById(42L)).thenReturn(owner);

        mockMvc.perform(post("/api/agent/notifications/email")
                        .requestAttr("agentId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Proposal\",\"body\":\"Review P-1\","
                                + "\"actionUrl\":\"/admin\"}"))
                .andExpect(status().isOk());

        verify(adminEmailService).sendToAdminOwner(42L, "[Agent: Cline] Proposal",
                "Agent Cline (ID 7) sent this notification.\n\nReview P-1", "/admin");
    }

    @Test
    void nonAdminOwnedAgentIsRejected() throws Exception {
        Agent agent = new Agent();
        agent.setId(7L);
        agent.setUserId(42L);
        User owner = new User();
        owner.setId(42L);
        owner.setRole("USER");
        owner.setStatus("ACTIVE");
        when(agentService.findById(7L)).thenReturn(agent);
        when(userService.findById(42L)).thenReturn(owner);

        mockMvc.perform(post("/api/agent/notifications/email")
                        .requestAttr("agentId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Proposal\",\"body\":\"Review P-1\"}"))
                .andExpect(status().isForbidden());
    }
}
