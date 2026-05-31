package com.ai.repo.controller;

import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.jwt.JwtProvider;
import com.ai.repo.service.TempTokenService;
import com.ai.repo.service.UserService;
import com.ai.repo.util.PasswordEncoderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {UserController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
@TestPropertySource(properties = "file.storage.base-path=/tmp/test-avatars")
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
                .andExpect(status().isOk())
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
                .andExpect(status().isOk())
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ===== A-01: Avatar upload returns new URL format =====

    @Test
    void uploadAvatar_shouldReturnNewUrlFormat() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setAvatar("/avatars/users/1/some-file.png");

        when(userService.update(any(User.class))).thenReturn(mockUser);

        BufferedImage img = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 300, 200);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile("avatar", "test.png", "image/png", imageBytes);

        mockMvc.perform(multipart("/api/users/1/avatar")
                        .file(file)
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.avatar", startsWith("/avatars/users/1/")))
                .andExpect(jsonPath("$.data.avatar", not(containsString("/api/"))));
    }

    // ===== A-02: Avatar upload rejects wrong user =====

    @Test
    void uploadAvatar_shouldReturn403_whenWrongUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatar", "test.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/users/2/avatar")
                        .file(file)
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ===== A-03: Avatar upload rejects invalid file type =====

    @Test
    void uploadAvatar_shouldRejectNonImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatar", "test.txt", "text/plain", "not an image".getBytes());

        mockMvc.perform(multipart("/api/users/1/avatar")
                        .file(file)
                        .with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
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
}
