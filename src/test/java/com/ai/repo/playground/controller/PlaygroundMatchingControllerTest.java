package com.ai.repo.playground.controller;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.ai.repo.playground.service.PlaygroundMatchingService;
import com.ai.repo.exception.GlobalExceptionHandler;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class PlaygroundMatchingControllerTest {
    PlaygroundMatchingService service; MockMvc mvc;
    @BeforeEach void setup(){service=mock(PlaygroundMatchingService.class);mvc=MockMvcBuilders.standaloneSetup(new PlaygroundMatchingController(service)).setControllerAdvice(new PlaygroundRequestAdvice(),new GlobalExceptionHandler()).build();}
    @Test void agentIdentityCannotQueueOrCancelForOwner() throws Exception {
        mvc.perform(get("/api/playground/matching/agents/1").requestAttr("agentId",1L).requestAttr("userId",1L)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/playground/matching/agents/1").requestAttr("agentId",1L).requestAttr("userId",1L)).andExpect(status().isForbidden()); verifyNoInteractions(service);
    }
    @Test void malformedBriefIsRejectedWithoutExposingBody() throws Exception {
        mvc.perform(post("/api/playground/matching").requestAttr("userId",1L).contentType(MediaType.APPLICATION_JSON).content("{\"agentId\":1,\"private\":")).andExpect(status().isBadRequest()).andExpect(jsonPath("message").value("INVALID_PLAYGROUND_REQUEST")); verifyNoInteractions(service);
    }
    @Test void humanCanQueueWithoutPartnerId() throws Exception {
        mvc.perform(post("/api/playground/matching").requestAttr("userId",1L).contentType(MediaType.APPLICATION_JSON).content("{\"agentId\":1,\"mode\":\"FULL\",\"ownerBrief\":{\"theme\":\"cats\",\"priority\":\"CHARACTER\",\"hardConstraints\":[],\"negotiable\":[],\"disclosableFields\":[]}}")).andExpect(status().isOk()); verify(service).enqueue(eq(1L),any());
    }
    @Test void missingOwnerIdentityIsRejected() throws Exception {
        mvc.perform(get("/api/playground/matching/agents/1")).andExpect(status().isUnauthorized()); verifyNoInteractions(service);
    }
}
