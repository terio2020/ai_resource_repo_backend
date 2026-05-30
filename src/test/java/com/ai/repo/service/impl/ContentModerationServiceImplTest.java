package com.ai.repo.service.impl;

import com.ai.repo.exception.ContentModerationException;
import com.ai.repo.exception.ContentModerationException.ModerationErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentModerationServiceImplTest {

    @Mock
    private MarkdownSecurityService markdownSecurityService;

    @Mock
    private OpenAIModerationService openAIModerationService;

    private ContentModerationServiceImpl contentModerationService;

    @BeforeEach
    void setUp() throws Exception {
        contentModerationService = new ContentModerationServiceImpl();

        // Inject mocks via reflection
        java.lang.reflect.Field markdownField = ContentModerationServiceImpl.class.getDeclaredField("markdownSecurityService");
        markdownField.setAccessible(true);
        markdownField.set(contentModerationService, markdownSecurityService);

        java.lang.reflect.Field openAiField = ContentModerationServiceImpl.class.getDeclaredField("openAIModerationService");
        openAiField.setAccessible(true);
        openAiField.set(contentModerationService, openAIModerationService);
    }

    // ===== 正常流程测试 =====

    @Test
    void moderateContent_shouldPass_whenBothServicesPass() {
        // Given
        String content = "Clean content for testing";
        String fileName = "clean.md";
        doNothing().when(markdownSecurityService).moderateContent(content, fileName);
        doNothing().when(openAIModerationService).moderateContent(content, fileName);

        // When & Then
        assertDoesNotThrow(() -> contentModerationService.moderateContent(content, fileName));

        verify(markdownSecurityService).moderateContent(content, fileName);
        verify(openAIModerationService).moderateContent(content, fileName);
    }

    @Test
    void moderateContent_shouldCallMarkdownFirst() {
        // Given
        String content = "Test content";
        String fileName = "test.md";
        doNothing().when(markdownSecurityService).moderateContent(content, fileName);
        doNothing().when(openAIModerationService).moderateContent(content, fileName);

        // When
        contentModerationService.moderateContent(content, fileName);

        // Then - verify order: markdown should be called before openai
        var inOrder = inOrder(markdownSecurityService, openAIModerationService);
        inOrder.verify(markdownSecurityService).moderateContent(content, fileName);
        inOrder.verify(openAIModerationService).moderateContent(content, fileName);
    }

    // ===== MarkdownSecurityService 失败测试 =====

    @Test
    void moderateContent_shouldThrow_whenMarkdownDetectsImage() {
        // Given
        String content = "Image: ![](https://example.com/img.png)";
        String fileName = "image.md";
        doThrow(new ContentModerationException(ModerationErrorType.IMAGE_NOT_ALLOWED, "![Alt](url)"))
                .when(markdownSecurityService).moderateContent(content, fileName);

        // When & Then
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> contentModerationService.moderateContent(content, fileName));
        assertEquals(ModerationErrorType.IMAGE_NOT_ALLOWED, exception.getErrorType());

        // OpenAI should NOT be called if markdown fails first
        verify(openAIModerationService, never()).moderateContent(any(), any());
    }

    @Test
    void moderateContent_shouldThrow_whenMarkdownDetectsXSS() {
        // Given
        String content = "<script>alert('xss')</script>";
        String fileName = "xss.html";
        doThrow(new ContentModerationException(ModerationErrorType.XSS_DETECTED, "<script>"))
                .when(markdownSecurityService).moderateContent(content, fileName);

        // When & Then
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> contentModerationService.moderateContent(content, fileName));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());

        verify(openAIModerationService, never()).moderateContent(any(), any());
    }

    @Test
    void moderateContent_shouldThrow_whenMarkdownDetectsSSRF() {
        // Given
        String content = "Access internal: http://192.168.1.1/admin";
        String fileName = "ssrf.md";
        doThrow(new ContentModerationException(ModerationErrorType.SSRF_DETECTED, "192.168.1.1"))
                .when(markdownSecurityService).moderateContent(content, fileName);

        // When & Then
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> contentModerationService.moderateContent(content, fileName));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());

        verify(openAIModerationService, never()).moderateContent(any(), any());
    }

    // ===== OpenAIModerationService 失败测试 =====

    @Test
    void moderateContent_shouldThrow_whenOpenAIDetectsSensitiveContent() {
        // Given
        String content = "Hateful content here";
        String fileName = "bad.md";
        doNothing().when(markdownSecurityService).moderateContent(content, fileName);
        doThrow(new ContentModerationException(ModerationErrorType.SENSITIVE_CONTENT, "hate"))
                .when(openAIModerationService).moderateContent(content, fileName);

        // When & Then
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> contentModerationService.moderateContent(content, fileName));
        assertEquals(ModerationErrorType.SENSITIVE_CONTENT, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenOpenAIApiError() {
        // Given
        String content = "Content that triggers API error";
        String fileName = "api-error.md";
        doNothing().when(markdownSecurityService).moderateContent(content, fileName);
        doThrow(new ContentModerationException(ModerationErrorType.MODERATION_API_ERROR))
                .when(openAIModerationService).moderateContent(content, fileName);

        // When & Then
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> contentModerationService.moderateContent(content, fileName));
        assertEquals(ModerationErrorType.MODERATION_API_ERROR, exception.getErrorType());
    }

    // ===== 流水线顺序测试 =====

    @Test
    void moderateContent_shouldFailFast_onMarkdownError() {
        // Given - markdown throws, openai should never be called
        String content = "Bad content";
        String fileName = "bad.md";
        doThrow(new ContentModerationException(ModerationErrorType.XSS_DETECTED, "onclick="))
                .when(markdownSecurityService).moderateContent(content, fileName);

        // When & Then
        assertThrows(ContentModerationException.class,
                () -> contentModerationService.moderateContent(content, fileName));

        // Verify only markdown was called
        verify(markdownSecurityService, times(1)).moderateContent(content, fileName);
        verify(openAIModerationService, never()).moderateContent(any(), any());
    }

    @Test
    void moderateContent_shouldProceedToOpenAI_whenMarkdownPasses() {
        // Given - markdown passes, openai is called
        String content = "Content passing local checks";
        String fileName = "pass.md";
        doNothing().when(markdownSecurityService).moderateContent(content, fileName);
        doNothing().when(openAIModerationService).moderateContent(content, fileName);

        // When
        contentModerationService.moderateContent(content, fileName);

        // Then - both should be called
        verify(markdownSecurityService).moderateContent(content, fileName);
        verify(openAIModerationService).moderateContent(content, fileName);
    }

    // ===== 错误消息传播测试 =====

    @Test
    void moderateContent_shouldPreserveErrorMessage_whenMarkdownFails() {
        // Given
        String detail = "forbidden pattern: javascript:";
        String content = "Click <a href=\"javascript:alert(1)\">here</a>";
        String fileName = "js-link.html";
        doThrow(new ContentModerationException(ModerationErrorType.XSS_DETECTED, detail))
                .when(markdownSecurityService).moderateContent(content, fileName);

        // When & Then
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> contentModerationService.moderateContent(content, fileName));
        assertTrue(exception.getMessage().contains(detail));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldPreserveErrorMessage_whenOpenAIFails() {
        // Given
        String categories = "harassment, hate";
        String content = "Bad content";
        String fileName = "bad.md";
        doNothing().when(markdownSecurityService).moderateContent(content, fileName);
        doThrow(new ContentModerationException(ModerationErrorType.SENSITIVE_CONTENT, categories))
                .when(openAIModerationService).moderateContent(content, fileName);

        // When & Then
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> contentModerationService.moderateContent(content, fileName));
        assertTrue(exception.getMessage().contains(categories));
        assertEquals(ModerationErrorType.SENSITIVE_CONTENT, exception.getErrorType());
    }
}