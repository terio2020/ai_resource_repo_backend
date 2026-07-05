package com.ai.repo.controller;

import com.ai.repo.entity.User;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.UserService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {AvatarController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
@TestPropertySource(properties = "file.storage.base-path=/tmp/test-avatars")
class AvatarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private RequestPostProcessor withUserId(Long userId) {
        return request -> {
            request.setAttribute("userId", userId);
            return request;
        };
    }

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

    @Test
    void uploadAvatar_shouldReturn403_whenWrongUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatar", "test.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/users/2/avatar")
                        .file(file)
                        .with(withUserId(1L)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void uploadAvatar_shouldRejectNonImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatar", "test.txt", "text/plain", "not an image".getBytes());

        mockMvc.perform(multipart("/api/users/1/avatar")
                        .file(file)
                        .with(withUserId(1L)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(400));
    }
}
