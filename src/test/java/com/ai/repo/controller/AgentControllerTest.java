package com.ai.repo.controller;

import com.ai.repo.entity.Agent;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.AgentService;
import com.ai.repo.util.ApiKeyUtil;
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

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {AgentController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
@TestPropertySource(properties = "file.storage.base-path=/tmp/test-agent-avatars-ctrl")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentService agentService;

    @MockBean
    private ApiKeyUtil apiKeyUtil;

    private RequestPostProcessor withAgentId(Long agentId) {
        return request -> {
            request.setAttribute("agentId", agentId);
            return request;
        };
    }

    @BeforeAll
    static void setUp() throws Exception {
        Files.createDirectories(Paths.get("/tmp/test-agent-avatars-ctrl/agents"));
    }

    @AfterAll
    static void tearDown() throws Exception {
        java.io.File dir = new java.io.File("/tmp/test-agent-avatars-ctrl");
        deleteDirectory(dir);
    }

    private static void deleteDirectory(java.io.File file) {
        if (file == null || !file.exists()) return;
        java.io.File[] files = file.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }

    @Test
    void uploadAvatar_shouldReturnNewUrlFormat() throws Exception {
        Agent mockAgent = new Agent();
        mockAgent.setId(1L);
        mockAgent.setAvatar("/avatars/agents/1/some-file.png");

        when(agentService.updateAvatar(eq(1L), anyString())).thenReturn(true);

        BufferedImage img = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 300, 200);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile("avatar", "test.png", "image/png", imageBytes);

        mockMvc.perform(multipart("/api/agents/1/avatar")
                        .file(file)
                        .with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.avatar", startsWith("/avatars/agents/1/")));
    }

    @Test
    void uploadAvatar_shouldReturn403_whenWrongAgent() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatar", "test.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/agents/2/avatar")
                        .file(file)
                        .with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void uploadAvatar_shouldRejectNonImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatar", "test.txt", "text/plain", "not an image".getBytes());

        mockMvc.perform(multipart("/api/agents/1/avatar")
                        .file(file)
                        .with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void uploadAvatar_shouldReturn403_whenNoAuth() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatar", "test.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/agents/1/avatar")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void getAvatar_shouldReturnImage_whenFileExists() throws Exception {
        String fileName = "test_avatar.png";
        java.nio.file.Path avatarDir = Paths.get("/tmp/test-agent-avatars-ctrl", "agents", "1");
        Files.createDirectories(avatarDir);
        java.nio.file.Path avatarFile = avatarDir.resolve(fileName);

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        ImageIO.write(img, "png", avatarFile.toFile());

        mockMvc.perform(get("/api/agents/1/avatar/" + fileName))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void getAvatar_shouldReturn404_whenFileNotFound() throws Exception {
        mockMvc.perform(get("/api/agents/999/avatar/nonexistent.png"))
                .andExpect(status().isNotFound());
    }
}
