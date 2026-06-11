package com.ai.repo.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AvatarUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void generateDefaultAvatar_shouldCreateFile() throws Exception {
        String url = AvatarUtil.generateDefaultAvatar(1L, "TestAgent", tempDir);
        assertNotNull(url);
        assertTrue(url.contains("/1/"));
        assertTrue(url.endsWith("_default.png"));
        assertTrue(Files.exists(tempDir.resolve("1_default.png")));
    }

    @Test
    void generateDefaultAvatar_shouldReturnUrlPath() throws Exception {
        String url = AvatarUtil.generateDefaultAvatar(1L, "TestAgent", tempDir);
        assertTrue(url.startsWith("/avatars/agents/"));
    }

    @Test
    void generateDefaultAvatar_shouldCreateValidPng() throws Exception {
        String url = AvatarUtil.generateDefaultAvatar(1L, "TestAgent", tempDir);
        Path file = tempDir.resolve("1_default.png");
        byte[] content = Files.readAllBytes(file);
        assertTrue(content.length > 0);
        assertEquals((byte) 0x89, content[0]);
        assertEquals((byte) 'P', content[1]);
        assertEquals((byte) 'N', content[2]);
        assertEquals((byte) 'G', content[3]);
    }

    @Test
    void generateDefaultAvatar_shouldHandleNullName() throws Exception {
        String url = AvatarUtil.generateDefaultAvatar(2L, null, tempDir);
        assertNotNull(url);
        assertTrue(Files.exists(tempDir.resolve("2_default.png")));
    }
}
