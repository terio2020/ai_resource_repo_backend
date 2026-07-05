package com.ai.repo.exception;

import com.ai.repo.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static Result<?> body(ResponseEntity<Result<?>> re) {
        return re.getBody();
    }

    @Test
    void handleBusinessException() {
        BusinessException e = new BusinessException(400, "Custom error");
        Result<?> result = body(handler.handleBusinessException(e));
        assertEquals(400, result.getCode());
        assertEquals("Custom error", result.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, handler.handleBusinessException(e).getStatusCode());
    }

    @Test
    void handleAuthenticationException() {
        AuthenticationException e = new AuthenticationException("Not authenticated");
        Result<?> result = body(handler.handleAuthenticationException(e));
        assertEquals(401, result.getCode());
        assertEquals("Not authenticated", result.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, handler.handleAuthenticationException(e).getStatusCode());
    }

    @Test
    void handleTokenExpiredException() {
        TokenExpiredException e = new TokenExpiredException("Token expired");
        Result<?> result = body(handler.handleTokenExpiredException(e));
        assertEquals(401, result.getCode());
        assertEquals("Token expired, please refresh", result.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, handler.handleTokenExpiredException(e).getStatusCode());
    }

    @Test
    void handleAccessDeniedException() {
        AccessDeniedException e = new AccessDeniedException("Access denied");
        Result<?> result = body(handler.handleAccessDeniedException(e));
        assertEquals(403, result.getCode());
        assertEquals("Access denied", result.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, handler.handleAccessDeniedException(e).getStatusCode());
    }

    @Test
    void handleValidationException() {
        MethodArgumentNotValidException e = org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);
        FieldError fieldError = new FieldError("obj", "field", "must not be null");
        org.mockito.Mockito.when(e.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getFieldError()).thenReturn(fieldError);

        Result<?> result = body(handler.handleValidationException(e));
        assertEquals(400, result.getCode());
        assertEquals("must not be null", result.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, handler.handleValidationException(e).getStatusCode());
    }

    @Test
    void handleBindException() {
        BindException e = org.mockito.Mockito.mock(BindException.class);
        org.springframework.validation.BindingResult bindingResult = org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);
        FieldError fieldError = new FieldError("obj", "field", "binding failed");
        org.mockito.Mockito.when(e.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getFieldError()).thenReturn(fieldError);

        Result<?> result = body(handler.handleBindException(e));
        assertEquals(400, result.getCode());
        assertEquals("binding failed", result.getMessage());
    }

    @Test
    void handleMissingParams() {
        MissingServletRequestParameterException e = new MissingServletRequestParameterException("userId", "Long");
        Result<?> result = body(handler.handleMissingParams(e));
        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("userId"));
    }

    @Test
    void handleIllegalArgumentException() {
        IllegalArgumentException e = new IllegalArgumentException("Invalid argument");
        Result<?> result = body(handler.handleIllegalArgumentException(e));
        assertEquals(400, result.getCode());
        assertEquals("Invalid argument", result.getMessage());
    }

    @Test
    void handleInvalidFileTypeException() {
        InvalidFileTypeException e = new InvalidFileTypeException("Only .md files allowed");
        Result<?> result = body(handler.handleInvalidFileTypeException(e));
        assertEquals(400, result.getCode());
        assertEquals("Only .md files allowed", result.getMessage());
    }

    @Test
    void handleFileTooLargeException() {
        FileTooLargeException e = new FileTooLargeException("File exceeds 50MB limit", 50 * 1024 * 1024);
        Result<?> result = body(handler.handleFileTooLargeException(e));
        assertEquals(413, result.getCode());
        assertEquals("File exceeds 50MB limit", result.getMessage());
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, handler.handleFileTooLargeException(e).getStatusCode());
    }

    @Test
    void handleFileStorageException() {
        FileStorageException e = new FileStorageException("Disk full");
        Result<?> result = body(handler.handleFileStorageException(e));
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("Disk full"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, handler.handleFileStorageException(e).getStatusCode());
    }

    @Test
    void handleRepositoryNotFoundException() {
        RepositoryNotFoundException e = new RepositoryNotFoundException("Repo not found");
        Result<?> result = body(handler.handleRepositoryNotFoundException(e));
        assertEquals(404, result.getCode());
    }

    @Test
    void handleFileNotAllowedException() {
        FileNotAllowedException e = new FileNotAllowedException("Path not allowed");
        Result<?> result = body(handler.handleFileNotAllowedException(e));
        assertEquals(400, result.getCode());
    }

    @Test
    void handleIOException() {
        IOException e = new IOException("I/O error");
        Result<?> result = body(handler.handleIOException(e));
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("I/O error"));
    }

    @Test
    void handleGitAPIException() {
        GitAPIException e = new GitAPIException("Git failed") {};
        Result<?> result = body(handler.handleGitAPIException(e));
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("Git failed"));
    }

    @Test
    void handleGenericException() {
        Exception e = new RuntimeException("Unexpected error");
        Result<?> result = body(handler.handleException(e));
        assertEquals(500, result.getCode());
        assertEquals("System error, please contact administrator", result.getMessage());
    }

    @Test
    void handleConstraintViolationException() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must be greater than or equal to 1");
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation);

        ConstraintViolationException e = new ConstraintViolationException("pathVariable.id", violations);
        Result<?> result = body(handler.handleConstraintViolation(e));
        assertEquals(400, result.getCode());
        assertEquals("must be greater than or equal to 1", result.getMessage());
    }

    @Test
    void handleConstraintViolationExceptionWithEmptyViolations() {
        ConstraintViolationException e = new ConstraintViolationException("no violations", Collections.emptySet());
        Result<?> result = body(handler.handleConstraintViolation(e));
        assertEquals(400, result.getCode());
        assertEquals("Validation failed", result.getMessage());
    }
}