package com.ai.repo.playground.controller;

import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.playground.service.PlaygroundShareService;
import com.ai.repo.security.RequireAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlaygroundAdminControllerTest {
    PlaygroundShareService shares;
    MockMvc mvc;

    @BeforeEach void setup() {
        shares=mock(PlaygroundShareService.class);
        mvc=MockMvcBuilders.standaloneSetup(new PlaygroundAdminController(shares))
                .setControllerAdvice(new PlaygroundRequestAdvice(),new GlobalExceptionHandler()).build();
    }

    @Test void endpointRequiresSharedAdminGuardAndRejectsAgentPrincipal() throws Exception {
        assertTrue(PlaygroundAdminController.class.getMethod("remove",long.class,
                PlaygroundAdminController.TakedownRequest.class,HttpServletRequest.class)
                .isAnnotationPresent(RequireAdmin.class));
        mvc.perform(post("/api/admin/playground/shares/23/remove")
                .requestAttr("userId",7L).requestAttr("agentId",101L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reasonCode\":\"PRIVACY\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(shares);
    }

    @Test void validAdminRequestPassesFiniteReasonAndIdentityToService() throws Exception {
        mvc.perform(post("/api/admin/playground/shares/23/remove")
                .requestAttr("userId",7L).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reasonCode\":\"PRIVACY\"}"))
                .andExpect(status().isOk());
        verify(shares).removePublishedResult(7L,23L,"PRIVACY");
    }

    @Test void invalidReasonAndAnonymousRequestCannotRemoveShare() throws Exception {
        mvc.perform(post("/api/admin/playground/shares/23/remove")
                .requestAttr("userId",7L).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reasonCode\":\"FREE_TEXT\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/playground/shares/23/remove")
                .requestAttr("userId",7L).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reasonCode\":\"PRIVACY\",\"userId\":999}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/playground/shares/23/remove")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reasonCode\":\"PRIVACY\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(shares);
    }

    @Test void adminRouteRemainsRegisteredWhenGameIsDisabled() {
        try (AnnotationConfigApplicationContext context=new AnnotationConfigApplicationContext()) {
            context.registerBean(PlaygroundShareService.class,()->shares);
            context.register(PlaygroundAdminController.class);
            context.refresh();
            assertTrue(context.containsBeanDefinition("playgroundAdminController"));
        }
    }
}
