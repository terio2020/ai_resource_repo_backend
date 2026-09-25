package com.ai.repo.config;

import com.ai.repo.jwt.JwtAuthenticationFilter;
import com.ai.repo.jwt.JwtProvider;
import com.ai.repo.service.AgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.ProbeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ContextConfiguration(classes = {
        SecurityConfigTest.ProbeController.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JwtProvider jwtProvider;
    @MockBean
    private AgentService agentService;

    @Test
    void unauthenticatedRequestDoesNotCreateSessionCookie() throws Exception {
        mockMvc.perform(get("/security-probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @RestController
    static class ProbeController {
        @GetMapping("/security-probe")
        String probe() {
            return "ok";
        }
    }
}
