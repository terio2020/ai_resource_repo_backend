package com.ai.repo.exception;

import com.ai.repo.common.Result;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessException() {
        BusinessException e = new BusinessException(400, "Custom error");
        Result<?> result = handler.handleBusinessException(e);
        assertEquals(400, result.getCode());
        assertEquals("Custom error", result.getMessage());
    }

    @Test
    void handleAuthenticationException() {
        AuthenticationException e = new AuthenticationException("Not authenticated");
        Result<?> result = handler.handleAuthenticationException(e);
        assertEquals(401, result.getCode());
        assertEquals("Not authenticated", result.getMessage());
    }

    @Test
    void handleTokenExpiredException() {
        TokenExpiredException e = new TokenExpiredException("Token expired");
        Result<?> result = handler.handleTokenExpiredException(e);
        assertEquals(401, result.getCode());
        assertEquals("Token expired, please refresh", result.getMessage());
    }

    @Test
    void handleAccessDeniedException() {
        AccessDeniedException e = new AccessDeniedException("Access denied");
        Result<?> result = handler.handleAccessDeniedException(e);
        assertEquals(403, result.getCode());
        assertEquals("Access denied", result.getMessage());
    }

    @Test
    void handleValidationException() {
        MethodArgumentNotValidException e = org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);
        FieldError fieldError = new FieldError("obj", "field", "must not be null");
        org.mockito.Mockito.when(e.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getFieldError()).thenReturn(fieldError);

        Result<?> result = handler.handleValidationException(e);
        assertEquals(400, result.getCode());
        assertEquals("must not be null", result.getMessage());
    }

    @Test
    void handleBindException() {
        BindException e = org.mockito.Mockito.mock(BindException.class);
        org.springframework.validation.BindingResult bindingResult = org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);
        FieldError fieldError = new FieldError("obj", "field", "binding failed");
        org.mockito.Mockito.when(e.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getFieldError()).thenReturn(fieldError);

        Result<?> result = handler.handleBindException(e);
        assertEquals(400, result.getCode());
        assertEquals("binding failed", result.getMessage());
    }

    @Test
    void handleMissingParams() {
        MissingServletRequestParameterException e = new MissingServletRequestParameterException("userId", "Long");
        Result<?> result = handler.handleMissingParams(e);
        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("userId"));
    }

    @Test
    void handleIllegalArgumentException() {
        IllegalArgumentException e = new IllegalArgumentException("Invalid argument");
        Result<?> result = handler.handleIllegalArgumentException(e);
        assertEquals(400, result.getCode());
        assertEquals("Invalid argument", result.getMessage());
    }

    @Test
    void handleInvalidFileTypeException() {
        InvalidFileTypeException e = new InvalidFileTypeException("Only .md files allowed");
        Result<?> result = handler.handleInvalidFileTypeException(e);
        assertEquals(400, result.getCode());
        assertEquals("Only .md files allowed", result.getMessage());
    }

    @Test
    void handleFileTooLargeException() {
        FileTooLargeException e = new FileTooLargeException("File exceeds 50MB limit", 50 * 1024 * 1024);
        Result<?> result = handler.handleFileTooLargeException(e);
        assertEquals(413, result.getCode());
        assertEquals("File exceeds 50MB limit", result.getMessage());
    }

    @Test
    void handleFileStorageException() {
        FileStorageException e = new FileStorageException("Disk full");
        Result<?> result = handler.handleFileStorageException(e);
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("Disk full"));
    }

    @Test
    void handleRepositoryNotFoundException() {
        RepositoryNotFoundException e = new RepositoryNotFoundException("Repo not found");
        Result<?> result = handler.handleRepositoryNotFoundException(e);
        assertEquals(404, result.getCode());
    }

    @Test
    void handleFileNotAllowedException() {
        FileNotAllowedException e = new FileNotAllowedException("Path not allowed");
        Result<?> result = handler.handleFileNotAllowedException(e);
        assertEquals(400, result.getCode());
    }

    @Test
    void handleIOException() {
        IOException e = new IOException("I/O error");
        Result<?> result = handler.handleIOException(e);
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("I/O error"));
    }

    @Test
    void handleGitAPIException() {
        GitAPIException e = new GitAPIException("Git failed") {};
        Result<?> result = handler.handleGitAPIException(e);
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("Git failed"));
    }

    @Test
    void handleGenericException() {
        Exception e = new RuntimeException("Unexpected error");
        Result<?> result = handler.handleException(e);
        assertEquals(500, result.getCode());
        assertEquals("System error, please contact administrator", result.getMessage());
    }
}
