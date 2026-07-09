package com.ai.repo.controller;

import com.ai.repo.dto.LoginResponse;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.jwt.JwtProvider;
import com.ai.repo.service.TempTokenService;
import com.ai.repo.service.UserService;
import com.ai.repo.util.PasswordEncoderUtil;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {UserController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoderUtil passwordEncoderUtil;

    @MockBean
    private TempTokenService tempTokenService;

    @MockBean
    private JwtProvider jwtProvider;

    private RequestPostProcessor withUserId(Long userId) {
        return request -> {
            request.setAttribute("userId", userId);
            return request;
        };
    }

    // ===== C-01: Update with nickname =====

    @Test
    void updateUser_shouldReturnUpdatedUser_whenNicknameProvided() throws Exception {
        // Given: simulate auth filter behavior by setting request attribute
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        updatedUser.setNickname("new-nick");
        updatedUser.setAvatar("old-avatar.png");

        when(userService.update(any(User.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(post("/api/users/update")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"new-nick\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.nickname").value("new-nick"));
    }

    // ===== C-02: Update email =====

    @Test
    void updateUser_shouldUpdateEmail_whenValid() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("new@example.com");
        updatedUser.setNickname("old-nickname");

        when(userService.update(any(User.class))).thenReturn(updatedUser);

        mockMvc.perform(post("/api/users/update")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.email").value("new@example.com"));
    }

    // ===== C-03: Email conflict =====

    @Test
    void updateUser_shouldThrowConflict_whenEmailTaken() throws Exception {
        when(userService.update(any(User.class)))
                .thenThrow(new BusinessException(409, "Email already exists"));

        mockMvc.perform(post("/api/users/update")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"taken@example.com\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    // ===== C-04: Update X fields =====

    @Test
    void updateUser_shouldUpdateXFields_whenProvided() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        updatedUser.setXHandle("newhandle");
        updatedUser.setXName("New X Name");
        updatedUser.setXAvatar("new-x-avatar.png");

        when(userService.update(any(User.class))).thenReturn(updatedUser);

        mockMvc.perform(post("/api/users/update")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"xHandle\":\"newhandle\",\"xName\":\"New X Name\",\"xAvatar\":\"new-x-avatar.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.xhandle").value("newhandle"))
                .andExpect(jsonPath("$.data.xname").value("New X Name"))
                .andExpect(jsonPath("$.data.xavatar").value("new-x-avatar.png"));
    }

    // ===== C-05: No auth =====

    @Test
    void updateUser_shouldReturn401_whenNoAuth() throws Exception {
        mockMvc.perform(post("/api/users/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"new-nick\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    // ===== C-06: User not found =====

    @Test
    void updateUser_shouldReturn404_whenUserNotFound() throws Exception {
        when(userService.update(any(User.class)))
                .thenThrow(new BusinessException(404, "User not found"));

        mockMvc.perform(post("/api/users/update")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"new-nick\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ===== L-01: Email login success =====

    @Test
    void loginByEmail_shouldReturnTokens_whenValidCredentials() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("user@example.com");

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setId(1L);
        loginResponse.setUsername("testuser");
        loginResponse.setEmail("user@example.com");
        loginResponse.setAccessToken("access-token-123");
        loginResponse.setRefreshToken("refresh-token-456");

        when(userService.findByEmail("user@example.com")).thenReturn(user);
        when(userService.verifyPasswordByEmail("user@example.com", "correct-password")).thenReturn(true);
        when(userService.generateTokens(1L)).thenReturn(loginResponse);

        mockMvc.perform(post("/api/users/login/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    // ===== L-02: Email login wrong password =====

    @Test
    void loginByEmail_shouldReturn401_whenWrongPassword() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        when(userService.findByEmail("user@example.com")).thenReturn(user);
        when(userService.verifyPasswordByEmail("user@example.com", "wrong-password")).thenReturn(false);

        mockMvc.perform(post("/api/users/login/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // ===== L-03: Email login non-existent user =====

    @Test
    void loginByEmail_shouldReturn401_whenEmailNotFound() throws Exception {
        when(userService.findByEmail("nonexistent@example.com")).thenReturn(null);

        mockMvc.perform(post("/api/users/login/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nonexistent@example.com\",\"password\":\"any-password\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // ===== L-04: Email login missing email =====

    @Test
    void loginByEmail_shouldReturn401_whenEmailMissing() throws Exception {
        mockMvc.perform(post("/api/users/login/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // ===== C-07: Partial update with single field =====

    @Test
    void updateUser_shouldAcceptPartialUpdate_withSingleField() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        updatedUser.setNickname("old-nickname");
        updatedUser.setAvatar("new-avatar.png");

        when(userService.update(any(User.class))).thenReturn(updatedUser);

        mockMvc.perform(post("/api/users/update")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatar\":\"new-avatar.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.avatar").value("new-avatar.png"))
                .andExpect(jsonPath("$.data.nickname").value("old-nickname"));
    }

    // ==================== POST /api/users (registration) ====================

    @Test
    void createUser_shouldEncodePasswordAndPersist() throws Exception {
        User created = new User();
        created.setId(99L);
        created.setUsername("newuser");
        when(userService.create(any(User.class))).thenReturn(created);
        when(passwordEncoderUtil.encode("raw-password")).thenReturn("ENCODED");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"raw-password\",\"email\":\"new@x.com\",\"nickname\":\"New U\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(passwordEncoderUtil).encode("raw-password");
        verify(userService).create(org.mockito.ArgumentMatchers.argThat(u ->
                "newuser".equals(u.getUsername())
                        && "ENCODED".equals(u.getPassword())
                        && "USER".equals(u.getRole())
                        && "ACTIVE".equals(u.getStatus())));
    }

    // ==================== POST /api/users/deleteById ====================

    @Test
    void deleteUser_shouldInvokeService() throws Exception {
        mockMvc.perform(post("/api/users/deleteById")
                        .param("id", "1")
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(userService).delete(1L);
    }

    // ==================== POST /api/users/login (username login) ====================

    @Test
    void login_shouldReturnTokens_whenValidCredentials() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setId(1L);
        loginResponse.setAccessToken("access-1");
        loginResponse.setRefreshToken("refresh-1");
        loginResponse.setUsername("testuser");

        when(userService.findByUsername("testuser")).thenReturn(user);
        when(userService.verifyPassword("testuser", "good-pass")).thenReturn(true);
        when(userService.generateTokens(1L)).thenReturn(loginResponse);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"good-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-1"));
        verify(userService).updateLoginTime(1L);
    }

    @Test
    void login_shouldReturn401_whenUserNotFound() throws Exception {
        when(userService.findByUsername("ghost")).thenReturn(null);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\",\"password\":\"any\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_shouldReturn401_whenWrongPassword() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        when(userService.findByUsername("testuser")).thenReturn(user);
        when(userService.verifyPassword("testuser", "wrong")).thenReturn(false);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"wrong\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ==================== POST /api/users/refresh-token ====================

    @Test
    void refreshToken_shouldDelegateToService() throws Exception {
        com.ai.repo.dto.TokenRefreshResponse resp = new com.ai.repo.dto.TokenRefreshResponse();
        resp.setAccessToken("new-access");
        resp.setRefreshToken("new-refresh");
        when(userService.refreshToken("old-refresh")).thenReturn(resp);

        mockMvc.perform(post("/api/users/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

    // ==================== POST /api/users/logout ====================

    @Test
    void logout_shouldClearTokens() throws Exception {
        mockMvc.perform(post("/api/users/logout").with(withUserId(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(userService).clearTokens(7L);
    }

    // ==================== POST /api/users/auth-login (agent session flow) ====================

    @Test
    void authLogin_shouldStoreTokenInTempTokenService() throws Exception {
        User user = new User();
        user.setId(3L);
        LoginResponse resp = new LoginResponse();
        resp.setAccessToken("agent-access-xxx");
        resp.setRefreshToken("agent-refresh-xxx");

        when(userService.findByUsername("agent-bot")).thenReturn(user);
        when(userService.verifyPassword("agent-bot", "secret")).thenReturn(true);
        when(userService.generateTokens(3L)).thenReturn(resp);

        mockMvc.perform(post("/api/users/auth-login")
                        .param("sessionId", "sess-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"agent-bot\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tempTokenService).storeToken("sess-001", "agent-access-xxx");
    }

    @Test
    void authLogin_shouldReturn401_whenInvalidCredentials() throws Exception {
        when(userService.findByUsername("agent-bot")).thenReturn(null);

        mockMvc.perform(post("/api/users/auth-login")
                        .param("sessionId", "sess-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"agent-bot\",\"password\":\"bad\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ==================== GET /api/users/me ====================

    @Test
    void getCurrentUser_shouldReturnUser_whenBearerTokenValid() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("secret");
        user.setAccessToken("tok");
        user.setRefreshToken("rtok");
        when(jwtProvider.validateAccessToken("valid-token")).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                // sensitive fields must be cleared before responding
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    void getCurrentUser_shouldReturn401_whenMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Missing or invalid Authorization header"));
    }

    @Test
    void getCurrentUser_shouldReturn401_whenTokenInvalid() throws Exception {
        when(jwtProvider.validateAccessToken("bad-token")).thenReturn(null);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer bad-token"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    void getCurrentUser_shouldReturn404_whenUserMissing() throws Exception {
        when(jwtProvider.validateAccessToken("valid-token")).thenReturn(999L);
        when(userService.findById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer valid-token"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ==================== Update with username ====================

    @Test
    void updateUser_shouldUpdateUsername_whenProvided() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("renamed-user");
        updatedUser.setEmail("test@example.com");
        updatedUser.setNickname("nick");

        when(userService.update(any(User.class))).thenReturn(updatedUser);

        mockMvc.perform(post("/api/users/update")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"renamed-user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("renamed-user"));
    }

    // ==================== Password update clears sensitive fields ====================

    @Test
    void updateUser_shouldStripSensitiveFieldsFromResponse() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        updatedUser.setNickname("nick");
        updatedUser.setPassword("ENCODED-FROM-SERVICE");
        updatedUser.setAccessToken("leaked-access");
        updatedUser.setRefreshToken("leaked-refresh");

        when(userService.update(any(User.class))).thenReturn(updatedUser);

        mockMvc.perform(post("/api/users/update")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"new-nick\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    // ==================== Update with no body ====================

    @Test
    void updateUser_shouldReturn401_whenUserIdMissing() throws Exception {
        mockMvc.perform(post("/api/users/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"x\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    // ==================== POST /api/users/password/change ====================

    @Test
    void changePassword_shouldSucceed_whenCurrentPasswordMatches() throws Exception {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("testuser");
        existing.setPassword("ENCODED-OLD-PASS");

        when(userService.findById(1L)).thenReturn(existing);
        when(passwordEncoderUtil.matches("correct-old-pass", "ENCODED-OLD-PASS")).thenReturn(true);
        when(userService.update(any(User.class))).thenReturn(existing);

        mockMvc.perform(post("/api/users/password/change")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"correct-old-pass\",\"newPassword\":\"new-secure-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(userService).update(argThat(u ->
                1L == u.getId() && "new-secure-pass".equals(u.getPassword())));
    }

    @Test
    void changePassword_shouldReturn400_whenCurrentPasswordWrong() throws Exception {
        User existing = new User();
        existing.setId(1L);
        existing.setPassword("ENCODED-OLD-PASS");

        when(userService.findById(1L)).thenReturn(existing);
        when(passwordEncoderUtil.matches("wrong-pass", "ENCODED-OLD-PASS")).thenReturn(false);

        mockMvc.perform(post("/api/users/password/change")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-pass\",\"newPassword\":\"new-secure-pass\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    void changePassword_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/users/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"any\",\"newPassword\":\"new-secure-pass\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void changePassword_shouldReturn404_whenUserNotFound() throws Exception {
        when(userService.findById(999L)).thenReturn(null);

        mockMvc.perform(post("/api/users/password/change")
                        .with(withUserId(999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"any\",\"newPassword\":\"new-secure-pass\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
