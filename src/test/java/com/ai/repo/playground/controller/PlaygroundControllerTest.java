package com.ai.repo.playground.controller;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.ai.repo.entity.Agent;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.playground.dto.PlaygroundRequests.*;
import com.ai.repo.playground.service.PlaygroundService;
import com.ai.repo.security.ApiKeyInterceptor;
import com.ai.repo.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlaygroundControllerTest {
    PlaygroundService service; AgentService identities; MockMvc mvc;
    @BeforeEach void setup() {
        service=mock(PlaygroundService.class); identities=mock(AgentService.class);
        Agent agent=new Agent(); agent.setId(101L); agent.setUserId(7L); agent.setChallengeVerified(true);
        when(identities.findById(101L)).thenReturn(agent); when(identities.findByApiKey("fixture-key")).thenReturn(agent);
        ApiKeyInterceptor auth=new ApiKeyInterceptor(); ReflectionTestUtils.setField(auth,"agentService",identities);
        ReflectionTestUtils.setField(auth,"objectMapper",new ObjectMapper());
        mvc=MockMvcBuilders.standaloneSetup(new PlaygroundController(service)).addInterceptors(auth)
                .setControllerAdvice(new PlaygroundRequestAdvice(),new GlobalExceptionHandler()).build();
    }
    @Test void noCredentialsCannotReadTasksOrIssuePermission() throws Exception {
        mvc.perform(get("/api/playground/agent/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/playground/agents/101/participation")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void humanJwtCannotImpersonateAgentTaskActor() throws Exception {
        mvc.perform(get("/api/playground/agent/tasks").requestAttr("userId",7L)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void agentKeyCannotUpdateOwnerPermissionEvenForSameOwner() throws Exception {
        mvc.perform(put("/api/playground/agents/101/participation").header("agent-auth-api-key","fixture-key")
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0,\"enabled\":true,\"maxDecisions\":4,\"maxAttempts\":4,\"maxDailyAttempts\":8}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("message").value("HUMAN_JWT_REQUIRED"));
        verifyNoInteractions(service);
    }
    @Test void authenticatedAgentIdentityComesFromInterceptor() throws Exception {
        when(service.tasks(101L)).thenReturn(List.of());
        mvc.perform(get("/api/playground/agent/tasks").header("agent-auth-api-key","fixture-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("code").value(200));
        verify(service).tasks(101L);
    }
    @Test void unverifiedAgentCannotReadPlaygroundTasks() throws Exception {
        Agent unverified=new Agent(); unverified.setId(101L); unverified.setUserId(7L); unverified.setChallengeVerified(false);
        when(identities.findByApiKey("fixture-key")).thenReturn(unverified);
        mvc.perform(get("/api/playground/agent/tasks").header("agent-auth-api-key","fixture-key")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void budgetsAndOwnerBriefsAreValidatedBeforeService() throws Exception {
        mvc.perform(put("/api/playground/agents/101/participation").requestAttr("userId",7L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0,\"enabled\":true,\"maxDecisions\":100,\"maxAttempts\":4,\"maxDailyAttempts\":8}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/playground/intentions").requestAttr("userId",7L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"agentId\":101,\"partnerAgentId\":102,\"mode\":\"FULL\",\"ownerBrief\":{\"theme\":\"cats\",\"priority\":\"invented\",\"hardConstraints\":[],\"negotiable\":[],\"disclosableFields\":[]}}"))
                .andExpect(status().isBadRequest()); verifyNoInteractions(service);
    }
    @Test void malformedLeaseJsonReturnsSafeFixedMessage() throws Exception {
        mvc.perform(post("/api/playground/agent/tasks/1/actions").header("agent-auth-api-key","fixture-key")
                .contentType(MediaType.APPLICATION_JSON).content("{\"leaseToken\":\"private-fixture-token\","))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").value("INVALID_PLAYGROUND_REQUEST"));
        verifyNoInteractions(service);
    }
    @Test void submissionRequiresServerAttemptAndUuid() throws Exception {
        mvc.perform(post("/api/playground/agent/tasks/1/actions").header("agent-auth-api-key","fixture-key")
                .contentType(MediaType.APPLICATION_JSON).content("{\"taskId\":\"1\",\"activityId\":\"1\",\"permissionVersion\":1,\"leaseToken\":\"fixture-long-token\",\"idempotencyKey\":\"bad\",\"action\":{}}"))
                .andExpect(status().isBadRequest()); verifyNoInteractions(service);
    }
    @Test void definiteFailureRequiresAgentKeyAndAllowlistedReason() throws Exception {
        String body="{\"leaseToken\":\"fixture-long-token\",\"permissionVersion\":1,\"attemptId\":\"3\",\"reasonCode\":\"MODEL_OUTPUT_INVALID\"}";
        mvc.perform(post("/api/playground/agent/tasks/1/failures").requestAttr("userId",7L)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/playground/agent/tasks/1/failures").header("agent-auth-api-key","fixture-key")
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("MODEL_OUTPUT_INVALID","MODEL_OUTCOME_UNKNOWN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
        ObjectNode response=new ObjectMapper().createObjectNode().put("status","INTERRUPTED");
        when(service.reportAttemptFailure(eq(101L),eq(1L),any(AttemptFailure.class))).thenReturn(response);
        mvc.perform(post("/api/playground/agent/tasks/1/failures").header("agent-auth-api-key","fixture-key")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("data.status").value("INTERRUPTED"));
        verify(service).reportAttemptFailure(eq(101L),eq(1L),any(AttemptFailure.class));
    }
    @Test void featureFlagHidesControllerByDefault() {
        try (AnnotationConfigApplicationContext context=new AnnotationConfigApplicationContext()) {
            context.registerBean(PlaygroundService.class,()->service); context.register(PlaygroundController.class); context.refresh();
            org.junit.jupiter.api.Assertions.assertEquals(0,context.getBeansOfType(PlaygroundController.class).size());
        }
    }
    @Test void explicitFeatureFlagRegistersController() {
        try (AnnotationConfigApplicationContext context=new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getSystemProperties().put("playground.enabled","true");
            try { context.registerBean(PlaygroundService.class,()->service); context.register(PlaygroundController.class); context.refresh();
                org.junit.jupiter.api.Assertions.assertEquals(1,context.getBeansOfType(PlaygroundController.class).size()); }
            finally { context.getEnvironment().getSystemProperties().remove("playground.enabled"); }
        }
    }
    @Test void strictDtoRejectsImpersonationFieldsEvenWhenBootIgnoresUnknownProperties() throws Exception {
        ObjectMapper lenient=new ObjectMapper().disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        org.junit.jupiter.api.Assertions.assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                ()->lenient.readValue("{\"permissionVersion\":1,\"agentId\":102}",Claim.class));
        mvc.perform(post("/api/playground/agent/tasks/1/claim").header("agent-auth-api-key","fixture-key")
                .contentType(MediaType.APPLICATION_JSON).content("{\"permissionVersion\":1,\"agentId\":102}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").value("INVALID_PLAYGROUND_REQUEST"));
        verifyNoInteractions(service);
    }
}
