package com.ai.repo.service.impl;

import com.ai.repo.exception.ContentModerationException;
import com.ai.repo.exception.ContentModerationException.ModerationErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenAIModerationService.
 * Focus on tests that don't require HTTP mocking:
 * - API key validation (skip when no key)
 * - JSON escaping utility method
 * - Error type messages
 */
class OpenAIModerationServiceTest {

    private OpenAIModerationService openAIModerationService;

    @BeforeEach
    void setUp() throws Exception {
        openAIModerationService = new OpenAIModerationService();
        java.lang.reflect.Field apiKeyField = OpenAIModerationService.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(openAIModerationService, "test-api-key");
    }

    @Test
    void moderateContent_shouldSkip_whenApiKeyIsBlank() throws Exception {
        OpenAIModerationService serviceNoKey = new OpenAIModerationService();
        java.lang.reflect.Field apiKeyField = OpenAIModerationService.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(serviceNoKey, "");

        assertDoesNotThrow(() -> serviceNoKey.moderateContent("any content", "test.md"));
    }

    @Test
    void moderateContent_shouldSkip_whenApiKeyIsNull() throws Exception {
        OpenAIModerationService serviceNoKey = new OpenAIModerationService();
        java.lang.reflect.Field apiKeyField = OpenAIModerationService.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(serviceNoKey, (String) null);

        assertDoesNotThrow(() -> serviceNoKey.moderateContent("any content", "test.md"));
    }

    @Test
    void moderateContent_shouldSkip_whenApiKeyIsPlaceholder() throws Exception {
        OpenAIModerationService serviceNoKey = new OpenAIModerationService();
        java.lang.reflect.Field apiKeyField = OpenAIModerationService.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(serviceNoKey, "   ");

        assertDoesNotThrow(() -> serviceNoKey.moderateContent("any content", "test.md"));
    }

    @Test
    void escapeJson_shouldEscapeBackslash() {
        try {
            java.lang.reflect.Method escapeJson = OpenAIModerationService.class.getDeclaredMethod("escapeJson", String.class);
            escapeJson.setAccessible(true);

            String result = (String) escapeJson.invoke(openAIModerationService, "path\\to\\file");
            assertTrue(result.contains("\\\\"));
        } catch (Exception e) {
            fail("Failed to test escapeJson: " + e.getMessage());
        }
    }

    @Test
    void escapeJson_shouldEscapeQuotes() {
        try {
            java.lang.reflect.Method escapeJson = OpenAIModerationService.class.getDeclaredMethod("escapeJson", String.class);
            escapeJson.setAccessible(true);

            String result = (String) escapeJson.invoke(openAIModerationService, "say \"hello\"");
            assertTrue(result.contains("\\\""));
        } catch (Exception e) {
            fail("Failed to test escapeJson: " + e.getMessage());
        }
    }

    @Test
    void escapeJson_shouldEscapeNewlines() {
        try {
            java.lang.reflect.Method escapeJson = OpenAIModerationService.class.getDeclaredMethod("escapeJson", String.class);
            escapeJson.setAccessible(true);

            String result = (String) escapeJson.invoke(openAIModerationService, "line1\nline2");
            assertTrue(result.contains("\\n"));
        } catch (Exception e) {
            fail("Failed to test escapeJson: " + e.getMessage());
        }
    }

    @Test
    void escapeJson_shouldEscapeCarriageReturn() {
        try {
            java.lang.reflect.Method escapeJson = OpenAIModerationService.class.getDeclaredMethod("escapeJson", String.class);
            escapeJson.setAccessible(true);

            String result = (String) escapeJson.invoke(openAIModerationService, "line1\rline2");
            assertTrue(result.contains("\\r"));
        } catch (Exception e) {
            fail("Failed to test escapeJson: " + e.getMessage());
        }
    }

    @Test
    void escapeJson_shouldEscapeTab() {
        try {
            java.lang.reflect.Method escapeJson = OpenAIModerationService.class.getDeclaredMethod("escapeJson", String.class);
            escapeJson.setAccessible(true);

            String result = (String) escapeJson.invoke(openAIModerationService, "col1\tcol2");
            assertTrue(result.contains("\\t"));
        } catch (Exception e) {
            fail("Failed to test escapeJson: " + e.getMessage());
        }
    }

    @Test
    void escapeJson_shouldHandleEmptyString() {
        try {
            java.lang.reflect.Method escapeJson = OpenAIModerationService.class.getDeclaredMethod("escapeJson", String.class);
            escapeJson.setAccessible(true);

            String result = (String) escapeJson.invoke(openAIModerationService, "");
            assertEquals("", result);
        } catch (Exception e) {
            fail("Failed to test escapeJson: " + e.getMessage());
        }
    }

    @Test
    void escapeJson_shouldHandleStringWithNoSpecialChars() {
        try {
            java.lang.reflect.Method escapeJson = OpenAIModerationService.class.getDeclaredMethod("escapeJson", String.class);
            escapeJson.setAccessible(true);

            String result = (String) escapeJson.invoke(openAIModerationService, "simple text no escaping");
            assertEquals("simple text no escaping", result);
        } catch (Exception e) {
            fail("Failed to test escapeJson: " + e.getMessage());
        }
    }

    @Test
    void constructor_shouldInitializeSuccessfully() {
        assertNotNull(new OpenAIModerationService());
    }

    @Test
    void moderationApiErrorType_shouldHaveCorrectMessage() {
        assertEquals("内容审核服务异常，请稍后重试", ModerationErrorType.MODERATION_API_ERROR.getMessage());
    }

    @Test
    void sensitiveContentErrorType_shouldHaveCorrectMessage() {
        assertEquals("内容包含敏感信息，请修改后重试", ModerationErrorType.SENSITIVE_CONTENT.getMessage());
    }
}